// Use UTC getters on the result to display the venue's Beijing date and time.
export function beijingCalendarTime(value) {
  let instant = value;
  if (typeof value === "string") {
    const local = /^(\d{4}-\d{2}-\d{2})(?:[ T](\d{2}:\d{2})(:\d{2}(?:\.\d+)?)?)?$/.exec(value);
    if (local) instant = local[1] + "T" + (local[2] || "00:00") + (local[3] || ":00") + "+08:00";
  }
  return new Date(new Date(instant).getTime() + 8 * 60 * 60 * 1000);
}

export function beijingDate(value) {
  if (value == null || value === "") return "";
  const date = beijingCalendarTime(value);
  return Number.isNaN(date.getTime()) ? "" : date.toISOString().slice(0, 10);
}
