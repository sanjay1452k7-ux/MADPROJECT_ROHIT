import { hash } from '@node-rs/argon2';
import {
  AttendanceMethod,
  AttendanceStatus,
  NotificationType,
  PrismaClient,
  SessionStatus,
  UserRole,
} from '@prisma/client';

/**
 * Seeds the demonstration dataset (Section 26).
 *
 * The roster, register numbers and attendance totals are the same figures the
 * Milestone 1 in-memory dataset used, so the application shows identical
 * numbers now that it is reading from PostgreSQL. Attendance history is
 * generated deterministically from the register number, which keeps demo runs
 * and screenshots reproducible.
 */
const prisma = new PrismaClient();

const EMAIL_DOMAIN = process.env.INSTITUTION_EMAIL_DOMAIN ?? 'citchennai.net';
const DEMO_PASSWORD = 'Cit@2026';

const SUBJECTS = [
  { code: 'CS8501', name: 'Mobile Application Development', held: 30 },
  { code: 'CS8502', name: 'Database Management Systems', held: 32 },
  { code: 'CS8503', name: 'Computer Networks', held: 28 },
  { code: 'CS8504', name: 'Operating Systems', held: 30 },
  { code: 'CS8505', name: 'Theory of Computation', held: 26 },
  { code: 'CS8506', name: 'Software Engineering', held: 28 },
];

const TOTAL_HELD = SUBJECTS.reduce((sum, s) => sum + s.held, 0);

/** [register number, name, classes attended out of TOTAL_HELD]. */
const ROSTER: Array<[string, string, number]> = [
  ['24CS0801', 'Aarthi Balasubramanian', 158],
  ['24CS0803', 'Abdul Rahman S', 149],
  ['24CS0805', 'Aishwarya Venkatesh', 161],
  ['24CS0807', 'Akash Nair', 128],
  ['24CS0809', 'Anitha Krishnan', 154],
  ['24CS0812', 'Rohit', 133],
  ['24CS0814', 'Bhavana Reddy', 145],
  ['24CS0816', 'Charan Kumar M', 118],
  ['24CS0818', 'Deepak Raj', 139],
  ['24CS0820', 'Divya Lakshmi', 165],
  ['24CS0822', 'Faizal Ahmed', 126],
  ['24CS0824', 'Gokul Prasad', 143],
  ['24CS0826', 'Harini Subramani', 157],
  ['24CS0828', 'Ishaan Mehta', 112],
  ['24CS0830', 'Janani Murugan', 151],
  ['24CS0832', 'Karthik Raja', 136],
  ['24CS0834', 'Keerthana S', 163],
  ['24CS0836', 'Lakshmi Priya', 147],
  ['24CS0838', 'Manoj Kumar', 122],
  ['24CS0840', 'Naveen Chandra', 141],
  ['24CS0842', 'Nithya Sree', 159],
  ['24CS0845', 'Sanjayan A', 152],
  ['24CS0847', 'Praveen Kumar', 130],
  ['24CS0849', 'Priya Darshini', 156],
  ['24CS0851', 'Rahul Menon', 138],
  ['24CS0853', 'Ramya Devi', 160],
  ['24CS0855', 'Sandhya Rajan', 148],
  ['24CS0857', 'Sathish Kumar', 108],
  ['24CS0859', 'Swetha Narayanan', 155],
  ['24CS0861', 'Vignesh Anand', 134],
  ['24CS0863', 'Yamini Raghavan', 150],
];

const TEACHERS = [
  {
    code: 'CIT-CSE-114',
    name: 'Dr. Meenakshi Sundaram',
    email: 'meenakshi.s',
    subjects: ['CS8501', 'CS8505'],
  },
  {
    code: 'CIT-CSE-127',
    name: 'Prof. Anitha Ramesh',
    email: 'anitha.r',
    subjects: ['CS8502', 'CS8506'],
  },
  {
    code: 'CIT-CSE-138',
    name: 'Dr. Karthikeyan V',
    email: 'karthikeyan.v',
    subjects: ['CS8503', 'CS8504'],
  },
];

/** Stable string hash, so a register number always yields the same figures. */
function seedOf(value: string): number {
  let hashValue = 0;
  for (let i = 0; i < value.length; i += 1) {
    hashValue = (hashValue * 31 + value.charCodeAt(i)) | 0;
  }
  return Math.abs(hashValue);
}

/**
 * Splits an attended total across subjects in proportion to classes held, then
 * nudges it per student so the table does not look uniformly generated. The
 * total is conserved exactly, which is what keeps per-subject figures
 * consistent with the overall percentage.
 */
function distribute(total: number, held: number[], seed: number): number[] {
  const attended = held.map((h) => Math.floor((h * total) / TOTAL_HELD));

  let remaining = total - attended.reduce((sum, n) => sum + n, 0);
  let step = 0;
  while (remaining > 0 && step < held.length * 4) {
    const index = (seed + step) % held.length;
    if (attended[index] < held[index]) {
      attended[index] += 1;
      remaining -= 1;
    }
    step += 1;
  }

  for (let index = 0; index < held.length; index += 2) {
    const to = (index + 3) % held.length;
    const shift = (seed >> (index + 1)) % 3;
    if (shift > 0 && attended[index] - shift >= 0 && attended[to] + shift <= held[to]) {
      attended[index] -= shift;
      attended[to] += shift;
    }
  }
  return attended;
}

/** Weekday dates going back from today, most recent first. */
function recentWeekdays(count: number): Date[] {
  const days: Date[] = [];
  const cursor = new Date();
  cursor.setHours(10, 15, 0, 0);
  while (days.length < count) {
    cursor.setDate(cursor.getDate() - 1);
    const day = cursor.getDay();
    if (day !== 0 && day !== 6) {
      days.push(new Date(cursor));
    }
  }
  return days;
}

async function main(): Promise<void> {
  console.log('Clearing existing data...');
  // Order matters: children before parents.
  await prisma.attendanceCorrection.deleteMany();
  await prisma.attendanceRecord.deleteMany();
  await prisma.attendanceToken.deleteMany();
  await prisma.attendanceSession.deleteMany();
  await prisma.timetableSlot.deleteMany();
  await prisma.enrollment.deleteMany();
  await prisma.teacherSubject.deleteMany();
  await prisma.notification.deleteMany();
  await prisma.auditLog.deleteMany();
  await prisma.refreshToken.deleteMany();
  await prisma.student.deleteMany();
  await prisma.teacher.deleteMany();
  await prisma.administrator.deleteMany();
  await prisma.user.deleteMany();
  await prisma.subject.deleteMany();
  await prisma.classSection.deleteMany();
  await prisma.department.deleteMany();
  await prisma.institution.deleteMany();

  const passwordHash = await hash(DEMO_PASSWORD);

  const institution = await prisma.institution.create({
    data: {
      name: 'Chennai Institute of Technology',
      code: 'CIT',
      emailDomain: EMAIL_DOMAIN,
    },
  });

  const department = await prisma.department.create({
    data: {
      institutionId: institution.id,
      name: 'Computer Science and Engineering',
      code: 'CSE',
    },
  });

  const classSection = await prisma.classSection.create({
    data: { departmentId: department.id, year: 3, section: 'A' },
  });

  console.log('Creating subjects...');
  const subjects = await Promise.all(
    SUBJECTS.map((subject) =>
      prisma.subject.create({
        data: { departmentId: department.id, code: subject.code, name: subject.name },
      }),
    ),
  );
  const subjectByCode = new Map(subjects.map((s) => [s.code, s]));

  console.log('Creating teachers...');
  const teacherRecords = [];
  for (const teacher of TEACHERS) {
    const user = await prisma.user.create({
      data: {
        institutionId: institution.id,
        email: teacher.email + '@' + EMAIL_DOMAIN,
        passwordHash,
        role: UserRole.TEACHER,
        displayName: teacher.name,
      },
    });
    const record = await prisma.teacher.create({
      data: {
        userId: user.id,
        departmentId: department.id,
        employeeCode: teacher.code,
        name: teacher.name,
        avatarSeed: teacher.code,
      },
    });
    await prisma.teacherSubject.createMany({
      data: teacher.subjects.map((code) => ({
        teacherId: record.id,
        subjectId: subjectByCode.get(code)!.id,
      })),
    });
    teacherRecords.push({ ...record, subjectCodes: teacher.subjects });
  }

  console.log('Creating administrator...');
  const adminUser = await prisma.user.create({
    data: {
      institutionId: institution.id,
      email: 'academics.office@' + EMAIL_DOMAIN,
      passwordHash,
      role: UserRole.ADMIN,
      displayName: 'Dr. Rajalakshmi Narayanan',
    },
  });
  await prisma.administrator.create({
    data: {
      userId: adminUser.id,
      name: 'Dr. Rajalakshmi Narayanan',
      designation: 'Director of Academics',
    },
  });

  console.log('Creating students and enrolments...');
  const heldCounts = SUBJECTS.map((s) => s.held);
  const students = [];

  for (const [registerNumber, name, attendedTotal] of ROSTER) {
    const user = await prisma.user.create({
      data: {
        institutionId: institution.id,
        email: registerNumber.toLowerCase() + '@' + EMAIL_DOMAIN,
        passwordHash,
        role: UserRole.STUDENT,
        displayName: name,
      },
    });
    const student = await prisma.student.create({
      data: {
        userId: user.id,
        classSectionId: classSection.id,
        registerNumber,
        name,
        avatarSeed: registerNumber,
      },
    });
    await prisma.enrollment.createMany({
      data: subjects.map((subject) => ({
        studentId: student.id,
        subjectId: subject.id,
        classSectionId: classSection.id,
      })),
    });
    students.push({ student, userId: user.id, attendedTotal, registerNumber });
  }

  console.log('Creating timetable...');
  const timetable = [
    { code: 'CS8501', start: '10:00', end: '10:50', room: 'Block C - 402' },
    { code: 'CS8502', start: '11:00', end: '11:50', room: 'Block C - 402' },
    { code: 'CS8503', start: '13:30', end: '14:20', room: 'Block C - 407' },
    { code: 'CS8504', start: '14:30', end: '15:20', room: 'Block C - 407' },
    { code: 'CS8505', start: '15:30', end: '16:20', room: 'Block C - 402' },
    { code: 'CS8506', start: '09:00', end: '09:50', room: 'Block C - 405' },
  ];

  for (let dayOfWeek = 1; dayOfWeek <= 6; dayOfWeek += 1) {
    for (const slot of timetable) {
      const subject = subjectByCode.get(slot.code)!;
      const teacher = teacherRecords.find((t) => t.subjectCodes.includes(slot.code))!;
      await prisma.timetableSlot.create({
        data: {
          classSectionId: classSection.id,
          subjectId: subject.id,
          teacherId: teacher.id,
          dayOfWeek,
          startTime: slot.start,
          endTime: slot.end,
          room: slot.room,
        },
      });
    }
  }

  console.log('Generating attendance history...');
  // One historical, closed session per class actually held, so the recorded
  // percentages are backed by real sessions rather than invented rows.
  const days = recentWeekdays(40);

  for (let subjectIndex = 0; subjectIndex < subjects.length; subjectIndex += 1) {
    const subject = subjects[subjectIndex];
    const held = heldCounts[subjectIndex];
    const teacher = teacherRecords.find((t) =>
      t.subjectCodes.includes(subject.code),
    )!;

    const attendedBySubject = new Map(
      students.map(({ student, attendedTotal, registerNumber }) => [
        student.id,
        distribute(attendedTotal, heldCounts, seedOf(registerNumber))[subjectIndex],
      ]),
    );

    // Each student gets their own rotation of the pattern so absences fall on
    // different days for different people.
    const rotation = new Map(
      students.map(({ student, registerNumber }) => [
        student.id,
        seedOf(registerNumber + subject.code) % held,
      ]),
    );

    for (let occurrence = 0; occurrence < held; occurrence += 1) {
      const when = days[occurrence % days.length];
      const startedAt = new Date(when);
      startedAt.setMinutes(startedAt.getMinutes() + subjectIndex * 5);

      const session = await prisma.attendanceSession.create({
        data: {
          classSectionId: classSection.id,
          subjectId: subject.id,
          teacherId: teacher.id,
          status: SessionStatus.CLOSED,
          startedAt,
          closedAt: new Date(startedAt.getTime() + 50 * 60 * 1000),
        },
      });

      const rows = students.map(({ student }) => {
        const attended = attendedBySubject.get(student.id) ?? 0;
        const slot = (occurrence + (rotation.get(student.id) ?? 0)) % held;
        // Bresenham spread: over a full cycle this marks exactly `attended`
        // classes present, with the absences distributed through the term
        // rather than bunched at one end.
        const present =
          Math.floor((slot * attended) / held) !==
          Math.floor(((slot + 1) * attended) / held);
        return {
          sessionId: session.id,
          studentId: student.id,
          subjectId: subject.id,
          status: present ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT,
          method: AttendanceMethod.QR_VERIFIED,
          markedAt: new Date(startedAt.getTime() + 12 * 60 * 1000),
          markedBy: teacher.id,
        };
      });

      await prisma.attendanceRecord.createMany({ data: rows });
    }
  }

  console.log('Creating notifications...');
  const sanjayan = students.find((s) => s.registerNumber === '24CS0845')!;
  await prisma.notification.createMany({
    data: [
      {
        userId: sanjayan.userId,
        type: NotificationType.ANNOUNCEMENT,
        title: 'Internal assessment schedule',
        message: 'The second internal assessment begins on 14 September.',
      },
    ],
  });

  const totals = await prisma.attendanceRecord.count();
  console.log('Seed complete.');
  console.log('  students: ' + students.length);
  console.log('  attendance records: ' + totals);
}

main()
  .catch((error) => {
    console.error(error);
    process.exit(1);
  })
  .finally(() => {
    void prisma.$disconnect();
  });
