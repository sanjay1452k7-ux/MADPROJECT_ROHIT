import { SetMetadata } from '@nestjs/common';
import { UserRole } from '@prisma/client';

export const ROLES_KEY = 'roles';

/**
 * Restricts a route to the listed roles. Enforcement is server-side and is the
 * only thing that decides access - the client hiding a button is not security
 * (Section 34).
 */
export const Roles = (...roles: UserRole[]) => SetMetadata(ROLES_KEY, roles);
