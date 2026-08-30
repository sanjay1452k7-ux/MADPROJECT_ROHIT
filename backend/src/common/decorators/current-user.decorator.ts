import { ExecutionContext, createParamDecorator } from '@nestjs/common';
import { UserRole } from '@prisma/client';

/** The identity carried by a verified access token. */
export interface AuthenticatedPrincipal {
  userId: string;
  email: string;
  role: UserRole;
  institutionId: string;
  /** Student, teacher or administrator row id for this user, when it exists. */
  profileId: string | null;
}

export const CurrentUser = createParamDecorator(
  (_data: unknown, ctx: ExecutionContext): AuthenticatedPrincipal => {
    const request = ctx.switchToHttp().getRequest();
    return request.user as AuthenticatedPrincipal;
  },
);
