import { getCurrentInstance, toRefs } from 'vue';

export function useFitnessDictOptions() {

  const { proxy } = getCurrentInstance() as ComponentInternalInstance;
  return toRefs<any>(
    proxy?.useDict(
      'fit_member_status',
      'fit_course_type',
      'fit_schedule_status',
      'fit_reservation_status',
      'fit_plan_type',
      'fit_card_status',
      'fit_order_type',
      'fit_order_status',
      'fit_order_source',
      'fit_pay_method',
      'fit_hire_status',
      'fit_reservation_source',
      'fit_consume_mode',
      'fit_checkin_status',
      'fit_checkin_type',
      'sys_normal_disable',
      'sys_user_sex'
    )
  );
}

export function findDictLabel(options: DictDataOption[] | undefined, value?: string | number | boolean | null) {
  if (value === undefined || value === null || value === '') {
    return '';
  }
  return options?.find((item) => String(item.value) === String(value))?.label ?? String(value);
}
