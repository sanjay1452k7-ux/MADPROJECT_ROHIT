import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
  Logger,
} from '@nestjs/common';
import { Response } from 'express';
import { ApiErrorCode } from './api-error';

/**
 * The last line of defence for Section 37.
 *
 * Whatever goes wrong, the client receives { code, message } and nothing else.
 * Stack traces, driver errors and internal messages are logged server-side and
 * never serialised into a response.
 */
@Catch()
export class AllExceptionsFilter implements ExceptionFilter {
  private readonly logger = new Logger(AllExceptionsFilter.name);

  catch(exception: unknown, host: ArgumentsHost): void {
    const response = host.switchToHttp().getResponse<Response>();

    if (exception instanceof HttpException) {
      const status = exception.getStatus();
      const body = exception.getResponse();

      // Framework exceptions (validation, auth, throttling) carry no code of
      // their own, so one is derived from the status. Without this a rate-limit
      // response would reach the client mislabelled as a validation failure.
      if (typeof body === 'object' && body !== null && !('code' in body)) {
        const messages = (body as { message?: string | string[] }).message;
        const message = Array.isArray(messages) ? messages[0] : messages;
        response.status(status).json({
          code: codeForStatus(status),
          message: messageForStatus(status, message),
        });
        return;
      }

      response.status(status).json(body);
      return;
    }

    this.logger.error('Unhandled exception', exception as Error);
    response.status(HttpStatus.INTERNAL_SERVER_ERROR).json({
      code: 'INTERNAL',
      message: 'Something went wrong. Please try again.',
    });
  }
}

function codeForStatus(status: number): ApiErrorCode {
  switch (status) {
    case HttpStatus.UNAUTHORIZED:
      return ApiErrorCode.SESSION_EXPIRED;
    case HttpStatus.FORBIDDEN:
      return ApiErrorCode.NOT_AUTHORISED;
    case HttpStatus.NOT_FOUND:
      return ApiErrorCode.NOT_FOUND;
    case HttpStatus.TOO_MANY_REQUESTS:
      return ApiErrorCode.RATE_LIMITED;
    default:
      return ApiErrorCode.VALIDATION;
  }
}

function messageForStatus(status: number, message?: string): string {
  if (status === HttpStatus.TOO_MANY_REQUESTS) {
    return 'Too many attempts. Please wait a moment and try again.';
  }
  if (status === HttpStatus.UNAUTHORIZED) {
    return 'Please sign in again to continue.';
  }
  return message ?? 'That request could not be processed.';
}
