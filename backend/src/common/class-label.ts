/**
 * Formats a cohort the way the product names it everywhere else:
 * "CSE - III Year - A". Roman numerals for the year are the convention used on
 * the student identity block, so the server must not emit "3 Year" instead.
 */
export function formatClassName(
  departmentCode: string,
  year: number,
  section: string,
): string {
  return [departmentCode, romanYear(year) + ' Year', section].join(' - ');
}

function romanYear(year: number): string {
  const numerals = ['I', 'II', 'III', 'IV', 'V'];
  return numerals[year - 1] ?? String(year);
}
