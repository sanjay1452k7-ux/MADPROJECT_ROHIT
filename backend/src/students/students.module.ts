import { Module } from '@nestjs/common';
import { AttendanceModule } from '../attendance/attendance.module';
import { StudentsController } from './students.controller';
import { StudentsService } from './students.service';

@Module({
  imports: [AttendanceModule],
  controllers: [StudentsController],
  providers: [StudentsService],
})
export class StudentsModule {}
