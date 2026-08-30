import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AttendanceStatus } from '@prisma/client';
import * as ExcelJS from 'exceljs';
import PDFDocument from 'pdfkit';
import { ApiError, ApiErrorCode } from '../common/api-error';
import { formatClassName } from '../common/class-label';
import { PrismaService } from '../common/prisma/prisma.service';

/** The report types the product offers (Section 33). */
export enum ReportType {
  STUDENT_ATTENDANCE = 'STUDENT_ATTENDANCE',
  SUBJECT_ATTENDANCE = 'SUBJECT_ATTENDANCE',
  CLASS_ATTENDANCE = 'CLASS_ATTENDANCE',
  LOW_ATTENDANCE = 'LOW_ATTENDANCE',
  ABSENTEE = 'ABSENTEE',
}

export enum ReportFormat {
  PDF = 'PDF',
  XLSX = 'XLSX',
}

export interface ReportRequest {
  type: ReportType;
  format: ReportFormat;
  /** Optional ISO dates. Absent means the whole term so far. */
  from?: string;
  to?: string;
  subjectId?: string;
  classSectionId?: string;
  studentId?: string;
}

export interface GeneratedReport {
  filename: string;
  contentType: string;
  body: Buffer;
}

/** A report's header block and rows, before it is rendered to a file. */
interface ReportModel {
  title: string;
  institution: string;
  department: string;
  className: string;
  subject: string;
  dateRange: string;
  generatedAt: string;
  summary: Array<[string, string]>;
  columns: string[];
  rows: string[][];
}

/**
 * Builds attendance reports (Section 33).
 *
 * The report model is assembled once and then rendered to either PDF or Excel,
 * so the two formats can never disagree about the figures - a difference
 * between them would be a reporting bug nobody would notice until an audit.
 */
@Injectable()
export class ReportsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly config: ConfigService,
  ) {}

  private get threshold(): number {
    return Number(this.config.get<string>('ATTENDANCE_THRESHOLD_PERCENT', '75'));
  }

  async generate(request: ReportRequest): Promise<GeneratedReport> {
    const model = await this.buildModel(request);

    if (request.format === ReportFormat.XLSX) {
      return {
        filename: this.filename(request, 'xlsx'),
        contentType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        body: await renderExcel(model),
      };
    }

    return {
      filename: this.filename(request, 'pdf'),
      contentType: 'application/pdf',
      body: await renderPdf(model),
    };
  }

  // ----------------------------------------------------------------- models

  private async buildModel(request: ReportRequest): Promise<ReportModel> {
    const institution = await this.prisma.institution.findFirst();
    if (!institution) {
      throw ApiError.notFound('No institution is configured.');
    }

    const { from, to } = parseRange(request.from, request.to);

    const records = await this.prisma.attendanceRecord.findMany({
      where: {
        markedAt: { gte: from, lte: to },
        ...(request.subjectId ? { subjectId: request.subjectId } : {}),
        ...(request.studentId ? { studentId: request.studentId } : {}),
        ...(request.classSectionId
          ? { student: { classSectionId: request.classSectionId } }
          : {}),
      },
      include: {
        subject: true,
        student: { include: { classSection: { include: { department: true } } } },
      },
      orderBy: [{ student: { registerNumber: 'asc' } }, { markedAt: 'asc' }],
    });

    const base = {
      institution: institution.name,
      department: records[0]?.student.classSection.department.name ?? 'All departments',
      className: records[0]
        ? formatClassName(
            records[0].student.classSection.department.code,
            records[0].student.classSection.year,
            records[0].student.classSection.section,
          )
        : 'All classes',
      subject: request.subjectId ? (records[0]?.subject.name ?? 'Subject') : 'All subjects',
      dateRange: `${formatDate(from)} to ${formatDate(to)}`,
      generatedAt: new Date().toLocaleString('en-GB'),
    };

    switch (request.type) {
      case ReportType.STUDENT_ATTENDANCE:
        return { ...base, ...this.studentModel(records) };
      case ReportType.SUBJECT_ATTENDANCE:
        return { ...base, ...this.subjectModel(records) };
      case ReportType.CLASS_ATTENDANCE:
        return { ...base, ...this.classModel(records) };
      case ReportType.LOW_ATTENDANCE:
        return { ...base, ...this.lowAttendanceModel(records) };
      case ReportType.ABSENTEE:
        return { ...base, ...this.absenteeModel(records) };
      default:
        throw new ApiError(ApiErrorCode.VALIDATION, 'Unknown report type.');
    }
  }

  /** Every recorded session for one student. */
  private studentModel(records: ReportRecords) {
    const held = records.length;
    const attended = records.filter((r) => r.status !== AttendanceStatus.ABSENT).length;

    return {
      title: 'Student Attendance Report',
      summary: summaryOf(held, attended, this.threshold),
      columns: ['Date', 'Subject', 'Code', 'Status', 'Marked at'],
      rows: records.map((r) => [
        formatDate(r.markedAt),
        r.subject.name,
        r.subject.code,
        r.status,
        r.status === AttendanceStatus.ABSENT ? '-' : formatTime(r.markedAt),
      ]),
    };
  }

  /** One row per student, for a single subject. */
  private subjectModel(records: ReportRecords) {
    const byStudent = groupByStudent(records);
    const held = records.length;
    const attended = records.filter((r) => r.status !== AttendanceStatus.ABSENT).length;

    return {
      title: 'Subject Attendance Report',
      summary: summaryOf(held, attended, this.threshold),
      columns: ['Register No.', 'Student', 'Held', 'Attended', 'Percentage'],
      rows: [...byStudent.values()].map((entry) => [
        entry.registerNumber,
        entry.name,
        String(entry.held),
        String(entry.attended),
        `${percentage(entry.attended, entry.held)}%`,
      ]),
    };
  }

  /** One row per student across every subject in the cohort. */
  private classModel(records: ReportRecords) {
    const byStudent = groupByStudent(records);
    const held = records.length;
    const attended = records.filter((r) => r.status !== AttendanceStatus.ABSENT).length;

    return {
      title: 'Class Attendance Report',
      summary: summaryOf(held, attended, this.threshold),
      columns: ['Register No.', 'Student', 'Held', 'Attended', 'Percentage', 'Standing'],
      rows: [...byStudent.values()].map((entry) => {
        const pct = percentage(entry.attended, entry.held);
        return [
          entry.registerNumber,
          entry.name,
          String(entry.held),
          String(entry.attended),
          `${pct}%`,
          pct >= this.threshold ? 'GOOD' : pct >= this.threshold - 10 ? 'WARNING' : 'HIGH RISK',
        ];
      }),
    };
  }

  /** Only students under the requirement, worst first. */
  private lowAttendanceModel(records: ReportRecords) {
    const byStudent = groupByStudent(records);
    const rows = [...byStudent.values()]
      .map((entry) => ({ ...entry, pct: percentage(entry.attended, entry.held) }))
      .filter((entry) => entry.held > 0 && entry.pct < this.threshold)
      .sort((a, b) => a.pct - b.pct);

    return {
      title: 'Low Attendance Report',
      summary: [
        ['Threshold', `${this.threshold}%`],
        ['Students below threshold', String(rows.length)],
        ['Students assessed', String(byStudent.size)],
      ] as Array<[string, string]>,
      columns: ['Register No.', 'Student', 'Held', 'Attended', 'Percentage', 'Shortfall'],
      rows: rows.map((entry) => [
        entry.registerNumber,
        entry.name,
        String(entry.held),
        String(entry.attended),
        `${entry.pct}%`,
        `${Math.round((this.threshold - entry.pct) * 10) / 10} points`,
      ]),
    };
  }

  /** Every individual absence in the range. */
  private absenteeModel(records: ReportRecords) {
    const absences = records.filter((r) => r.status === AttendanceStatus.ABSENT);

    return {
      title: 'Absentee Report',
      summary: [
        ['Sessions recorded', String(records.length)],
        ['Absences', String(absences.length)],
        [
          'Absence rate',
          `${records.length === 0 ? 0 : Math.round((absences.length / records.length) * 1000) / 10}%`,
        ],
      ] as Array<[string, string]>,
      columns: ['Date', 'Register No.', 'Student', 'Subject'],
      rows: absences.map((r) => [
        formatDate(r.markedAt),
        r.student.registerNumber,
        r.student.name,
        r.subject.name,
      ]),
    };
  }

  private filename(request: ReportRequest, extension: string): string {
    const stamp = new Date().toISOString().slice(0, 10);
    return `${request.type.toLowerCase()}-${stamp}.${extension}`;
  }
}

// ------------------------------------------------------------------ helpers

type ReportRecords = Array<{
  status: AttendanceStatus;
  markedAt: Date;
  subject: { code: string; name: string };
  student: {
    id: string;
    name: string;
    registerNumber: string;
    classSection: { year: number; section: string; department: { code: string; name: string } };
  };
}>;

function groupByStudent(records: ReportRecords) {
  const byStudent = new Map<
    string,
    { name: string; registerNumber: string; held: number; attended: number }
  >();

  for (const record of records) {
    const entry = byStudent.get(record.student.id) ?? {
      name: record.student.name,
      registerNumber: record.student.registerNumber,
      held: 0,
      attended: 0,
    };
    entry.held += 1;
    if (record.status !== AttendanceStatus.ABSENT) {
      entry.attended += 1;
    }
    byStudent.set(record.student.id, entry);
  }
  return byStudent;
}

function summaryOf(held: number, attended: number, threshold: number): Array<[string, string]> {
  const pct = percentage(attended, held);
  return [
    ['Sessions held', String(held)],
    ['Sessions attended', String(attended)],
    ['Attendance', `${pct}%`],
    ['Requirement', `${threshold}%`],
    ['Standing', pct >= threshold ? 'MEETS REQUIREMENT' : 'BELOW REQUIREMENT'],
  ];
}

function percentage(attended: number, held: number): number {
  return held === 0 ? 0 : Math.round((attended / held) * 1000) / 10;
}

function parseRange(from?: string, to?: string): { from: Date; to: Date } {
  const end = to ? new Date(to) : new Date();
  end.setHours(23, 59, 59, 999);

  const start = from ? new Date(from) : new Date(end.getFullYear(), 0, 1);
  start.setHours(0, 0, 0, 0);

  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    throw new ApiError(
      ApiErrorCode.VALIDATION,
      'The date range could not be understood.',
    );
  }
  return { from: start, to: end };
}

function formatDate(date: Date): string {
  return date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatTime(date: Date): string {
  return date.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
}

// ----------------------------------------------------------------- renderers

/** Renders the model to a PDF with the institution header Section 33 requires. */
function renderPdf(model: ReportModel): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({ size: 'A4', margin: 40 });
    const chunks: Buffer[] = [];

    doc.on('data', (chunk: Buffer) => chunks.push(chunk));
    doc.on('end', () => resolve(Buffer.concat(chunks)));
    doc.on('error', reject);

    doc.fontSize(16).text(model.institution, { align: 'center' });
    doc.moveDown(0.2);
    doc.fontSize(12).text(model.title, { align: 'center' });
    doc.moveDown(0.8);

    doc.fontSize(9);
    doc.text(`Department: ${model.department}`);
    doc.text(`Class: ${model.className}`);
    doc.text(`Subject: ${model.subject}`);
    doc.text(`Period: ${model.dateRange}`);
    doc.text(`Generated: ${model.generatedAt}`);
    doc.moveDown(0.6);

    for (const [label, value] of model.summary) {
      doc.text(`${label}: ${value}`);
    }
    doc.moveDown(0.6);

    const columnWidth = (doc.page.width - 80) / model.columns.length;
    let y = doc.y;

    const writeRow = (cells: string[], bold: boolean) => {
      // A new page when the current one runs out, so long rosters do not spill
      // off the bottom.
      if (y > doc.page.height - 60) {
        doc.addPage();
        y = 40;
      }
      doc.font(bold ? 'Helvetica-Bold' : 'Helvetica').fontSize(8);
      cells.forEach((cell, index) => {
        doc.text(cell, 40 + index * columnWidth, y, {
          width: columnWidth - 4,
          ellipsis: true,
        });
      });
      y += 14;
    };

    writeRow(model.columns, true);
    for (const row of model.rows) {
      writeRow(row, false);
    }

    doc.end();
  });
}

/** Renders the same model to a spreadsheet. */
async function renderExcel(model: ReportModel): Promise<Buffer> {
  const workbook = new ExcelJS.Workbook();
  workbook.creator = model.institution;
  workbook.created = new Date();

  const sheet = workbook.addWorksheet('Attendance');

  sheet.addRow([model.institution]);
  sheet.addRow([model.title]);
  sheet.addRow([]);
  sheet.addRow(['Department', model.department]);
  sheet.addRow(['Class', model.className]);
  sheet.addRow(['Subject', model.subject]);
  sheet.addRow(['Period', model.dateRange]);
  sheet.addRow(['Generated', model.generatedAt]);
  sheet.addRow([]);

  for (const [label, value] of model.summary) {
    sheet.addRow([label, value]);
  }
  sheet.addRow([]);

  const header = sheet.addRow(model.columns);
  header.font = { bold: true };

  for (const row of model.rows) {
    sheet.addRow(row);
  }

  sheet.columns.forEach((column) => {
    column.width = 22;
  });

  const buffer = await workbook.xlsx.writeBuffer();
  return Buffer.from(buffer);
}
