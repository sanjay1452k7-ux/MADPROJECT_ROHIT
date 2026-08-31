import { ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { NestFactory } from '@nestjs/core';
import { ServerResponse } from 'node:http';
import { AppModule } from './app.module';
import { AllExceptionsFilter } from './common/all-exceptions.filter';
import { cacheControlMiddleware } from './common/cache-control.middleware';
import { PrismaService } from './common/prisma/prisma.service';

async function bootstrap(): Promise<void> {
  const app = await NestFactory.create(AppModule);

  // The API is consumed by a native app, not a browser, so no origin needs
  // cross-origin access. An explicit allowlist is read from configuration for
  // the day an administrative web console is added (Section 34).
  const allowedOrigins = (process.env.CORS_ALLOWED_ORIGINS ?? '')
    .split(',')
    .map((origin) => origin.trim())
    .filter((origin) => origin.length > 0);
  app.enableCors(
    allowedOrigins.length > 0
      ? { origin: allowedOrigins, credentials: true }
      : { origin: false },
  );

  // Response headers. The API returns JSON and files, never HTML, so the
  // browser-facing surface is small - but a downloaded report should not be
  // sniffed into something executable, and nothing here belongs in a frame.
  app.use(cacheControlMiddleware);

  app.use((_request: unknown, response: ServerResponse, next: () => void) => {
    response.setHeader('X-Content-Type-Options', 'nosniff');
    response.setHeader('X-Frame-Options', 'DENY');
    response.setHeader('Referrer-Policy', 'no-referrer');
    response.setHeader('Cross-Origin-Resource-Policy', 'same-site');
    response.setHeader('Permissions-Policy', 'camera=(), geolocation=(), microphone=()');
    // Served over TLS in production; harmless when it is not.
    response.setHeader(
      'Strict-Transport-Security',
      'max-age=31536000; includeSubDomains',
    );
    next();
  });

  // The framework advertises itself by default, which only helps an attacker
  // match a known version to a known exploit.
  app.getHttpAdapter().getInstance().disable('x-powered-by');

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
