import { Global, Module } from '@nestjs/common';
import { AttendanceStatsService } from '../attendance-stats.service';
import { PrismaService } from './prisma.service';

@Global()
@Module({
  providers: [PrismaService, AttendanceStatsService],
  exports: [PrismaService, AttendanceStatsService],
})
export class PrismaModule {}
