import { ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { AllExceptionsFilter } from './common/all-exceptions.filter';
import { PrismaService } from './common/prisma/prisma.service';

async function bootstrap(): Promise<void> {
  const app = await NestFactory.create(AppModule);

  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      // Reject unknown fields outright rather than silently dropping them.
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );
  app.useGlobalFilters(new AllExceptionsFilter());

  const prisma = app.get(PrismaService);
  await prisma.enableShutdownHooks(app);

  const config = app.get(ConfigService);
  const port = Number(config.get<string>('PORT', '3000'));

  // 0.0.0.0 so an Android emulator can reach the service on 10.0.2.2.
  await app.listen(port, '0.0.0.0');
}

void bootstrap();
