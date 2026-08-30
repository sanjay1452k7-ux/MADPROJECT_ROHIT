import { HttpException, HttpStatus } from '@nestjs/common';

/**
 * Stable, machine-readable failure codes (Sections 20 and 37).
 *
 * The Android client maps these onto its own AppError type. They are part of
 * the API contract: the human-readable message may change wording freely, but a
 * code may not change meaning.
 */
export enum ApiErrorCode {
  INVALID_CREDENTIALS = 'INVALID_CREDENTIALS',
  ACCOUNT_INACTIVE = 'ACCOUNT_INACTIVE',
  ROLE_MISMATCH = 'ROLE_MISMATCH',
  SESSION_EXPIRED = 'SESSION_EXPIRED',
  NOT_AUTHORISED = 'NOT_AUTHORISED',
  NOT_FOUND = 'NOT_FOUND',
  VALIDATION = 'VALIDATION',
  RATE_LIMITED = 'RATE_LIMITED',

  QR_EXPIRED = 'QR_EXPIRED',
  QR_INVALID = 'QR_INVALID',
  QR_ALREADY_USED = 'QR_ALREADY_USED',
  SESSION_CLOSED = 'SESSION_CLOSED',
  NOT_ENROLLED = 'NOT_ENROLLED',
  ALREADY_MARKED = 'ALREADY_MARKED',
}

/**
 * An application failure with a stable code.
 *
 * Raw exceptions never reach the client: the response body is always
 * { code, message }, so no stack trace or driver detail can leak (Section 37).
 */
export class ApiError extends HttpException {
  constructor(
    readonly code: ApiErrorCode,
    message: string,
    status: HttpStatus = HttpStatus.BAD_REQUEST,
    readonly details?: Record<string, unknown>,
  ) {
    super({ code, message, ...(details ?? {}) }, status);
  }

  static invalidCredentials(): ApiError {
    return new ApiError(
      ApiErrorCode.INVALID_CREDENTIALS,
      'That email and password combination was not recognised.',
      HttpStatus.UNAUTHORIZED,
    );
  }

  static roleMismatch(actualRole: string): ApiError {
    return new ApiError(
      ApiErrorCode.ROLE_MISMATCH,
      'This account does not hold the selected role.',
      HttpStatus.FORBIDDEN,
      { actualRole },
    );
  }

  static accountInactive(reason: string): ApiError {
    return new ApiError(ApiErrorCode.ACCOUNT_INACTIVE, reason, HttpStatus.FORBIDDEN);
  }

  static notAuthorised(): ApiError {
    return new ApiError(
      ApiErrorCode.NOT_AUTHORISED,
      'Your account does not have access to this.',
      HttpStatus.FORBIDDEN,
    );
  }

  static notFound(message = 'We could not find that record.'): ApiError {
    return new ApiError(ApiErrorCode.NOT_FOUND, message, HttpStatus.NOT_FOUND);
  }

  static qrExpired(): ApiError {
    return new ApiError(ApiErrorCode.QR_EXPIRED, 'QR code expired.', HttpStatus.GONE);
  }

  static qrInvalid(): ApiError {
    return new ApiError(
      ApiErrorCode.QR_INVALID,
      'Invalid attendance token.',
      HttpStatus.BAD_REQUEST,
    );
  }

  static qrAlreadyUsed(): ApiError {
    return new ApiError(
      ApiErrorCode.QR_ALREADY_USED,
      'This QR code has already been scanned.',
      HttpStatus.CONFLICT,
    );
  }

  static sessionClosed(): ApiError {
    return new ApiError(
      ApiErrorCode.SESSION_CLOSED,
      'Attendance session closed.',
      HttpStatus.CONFLICT,
    );
  }

  static notEnrolled(): ApiError {
    return new ApiError(
      ApiErrorCode.NOT_ENROLLED,
      'Student is not enrolled in this subject.',
      HttpStatus.FORBIDDEN,
    );
  }

  static alreadyMarked(details: Record<string, unknown>): ApiError {
    return new ApiError(
      ApiErrorCode.ALREADY_MARKED,
      'Attendance already recorded for this student.',
      HttpStatus.CONFLICT,
      details,
    );
  }
}
