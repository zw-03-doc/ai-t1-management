import type { FitnessScheduleForm } from '@/api/fitness/schedule/types';

export interface SessionTime {
  date: string;
  start: string;
  end: string;
  nextDay: boolean;
  studentMemberIds?: (string | number)[];
}

function nextDate(date: string) {
  const day = new Date(date + 'T00:00:00Z');
  day.setUTCDate(day.getUTCDate() + 1);
  return day.toISOString().slice(0, 10);
}
function stamp(date: string, clock: string) {
  return date + ' ' + (clock.length === 5 ? clock + ':00' : clock);
}
function bounds(row: SessionTime) {
  return {
    scheduleDate: row.date,
    startTime: stamp(row.date, row.start),
    endTime: stamp(row.nextDay ? nextDate(row.date) : row.date, row.end)
  };
}

export function draftError(rows?: SessionTime[]) {
  if (!rows?.length) return '请选择日期并保留至少一个场次';
  const intervals: { startTime: string; endTime: string }[] = [];
  for (const row of rows) {
    if (!row.date || !row.start || !row.end) return '请填写每个时间段的开始和结束时间';
    const span = bounds(row);
    if (span.endTime <= span.startTime) return row.date + ' 的结束时间须晚于开始时间';
    if (row.nextDay && span.endTime.slice(11) > span.startTime.slice(11)) return row.date + ' 的场次超过24小时';
    intervals.push(span);
  }
  intervals.sort((a,b) => a.startTime.localeCompare(b.startTime));
  for (let i = 1; i < intervals.length; i++) {
    if (intervals[i].startTime < intervals[i - 1].endTime) return intervals[i].startTime.slice(0,16) + ' 与另一场时间重叠';
  }
  return '';
}

export function scheduleRequests(form: FitnessScheduleForm & { sessions: SessionTime[] }) {
  const { sessions, ...common } = form;
  const error = draftError(sessions);
  if (error) throw new Error(error);
  return sessions.map(row => ({
    ...common, ...bounds(row),
    scheduleNo: sessions.length === 1 ? common.scheduleNo : undefined,
    studentMemberIds: common.scheduleMode === 'normal' ? [] : (row.studentMemberIds ?? common.studentMemberIds)
  }));
}

export function timeRangeLabel(row: FitnessScheduleForm) {
  const { startTime, endTime } = row;
  if (!startTime || !endTime) return '—';
  return startTime.slice(11,16) + '–' + (endTime.slice(0,10) !== startTime.slice(0,10) ? '次日 ' : '') + endTime.slice(11,16);
}
