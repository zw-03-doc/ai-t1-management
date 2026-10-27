<template>
  <view class="page-wrap with-nav">
    <view class="eyebrow">T1 / CLASSES</view>
    <view class="page-title">让训练有个约定</view>
    <view class="page-subtitle">先了解课程，再选择适合你的日期与时段。</view>
    <scroll-view class="scroll tabs" scroll-x :show-scrollbar="false">
      <view
        class="chip"
        :class="{ active: currentTab === item.value }"
        v-for="item in tabs"
        :key="item.value"
        @tap="switchTab(item.value)"
        >{{ item.label }}</view
      >
    </scroll-view>
    <view class="section-head"
      ><text class="section-title">{{ sectionLabel }}</text
      ><text class="muted" v-if="currentTab === 'schedule'"
        >北京时间</text
      ></view
    >
    <view
      v-if="currentTab === 'schedule' && selectedCourseId"
      class="course-filter row"
      ><text>仅看 · {{ selectedCourseName || "所选课程" }}</text
      ><text class="clear-filter" @tap="clearCourseFilter">查看全部</text></view
    >
    <view v-if="currentTab === 'course'">
      <scroll-view class="scroll filters" scroll-x :show-scrollbar="false">
        <view
          class="chip"
          :class="{ active: courseType === '' }"
          @tap="changeCourseType('')"
          >全部</view
        >
        <view
          class="chip"
          :class="{ active: courseType === 'group' }"
          @tap="changeCourseType('group')"
          >团课</view
        >
        <view
          class="chip"
          :class="{ active: courseType === 'private' }"
          @tap="changeCourseType('private')"
          >私教</view
        >
        <view
          class="chip"
          :class="{ active: courseType === 'small' }"
          @tap="changeCourseType('small')"
          >小班</view
        >
        <view
          class="chip"
          :class="{ active: courseType === 'experience' }"
          @tap="changeCourseType('experience')"
          >体验</view
        >
      </scroll-view>
    </view>
    <view
      v-if="currentTab === 'schedule' && scheduleDates.length"
      class="date-section"
    >
      <scroll-view class="scroll" scroll-x :show-scrollbar="false">
        <view
          class="date-chip"
          :class="{ active: !selectedDate }"
          @tap="selectedDate = ''"
          ><text>日期</text><text class="date-day">全部</text></view
        >
        <view
          v-for="date in scheduleDates"
          :key="date"
          class="date-chip"
          :class="{ active: selectedDate === date }"
          @tap="selectedDate = date"
          ><text>{{ date.slice(0, 4) }}</text
          ><text class="date-day">{{
            date.slice(5).replace("-", " / ")
          }}</text></view
        >
      </scroll-view>
    </view>
    <view v-if="loading" class="empty">正在为你找课程…</view>
    <view v-else-if="errorText" class="empty error" @tap="loadCurrentTabData"
      ><text class="empty-title">暂时没能加载</text>{{ errorText
      }}<view class="retry">轻点重试</view></view
    >
    <block v-else>
      <block v-if="currentTab === 'course'">
        <view
          class="card course-card"
          v-for="item in courseList"
          :key="item.courseId"
          @tap="openCourse(item)"
        >
          <image
            v-if="item.coverUrl"
            class="course-cover"
            :src="item.coverUrl"
            mode="aspectFill"
          />
          <view class="row"
            ><text class="tag">{{ courseTypeText(item.courseType) }}</text
            ><text class="muted">{{
              item.durationMinutes == null
                ? "时长待定"
                : item.durationMinutes + " 分钟"
            }}</text></view
          >
          <view class="card-title">{{ item.courseName }}</view>
          <view class="card-desc"
            >{{ item.storeName || "门店待定" }}<text class="dot">·</text
            >{{ item.coachName || "教练待安排" }}</view
          >
          <view class="card-footer"
            ><view
              ><text class="price">{{ priceText(item.displayPrice) }}</text
              ><text class="price-caption">课程参考价</text></view
            ><text class="text-link">了解课程 →</text></view
          >
        </view>
        <view v-if="!courseList.length" class="empty"
          ><text class="empty-title">新课程正在准备</text
          >换个类型看看，或稍后再来。</view
        >
      </block>
      <block v-if="currentTab === 'schedule'">
        <view
          class="card schedule-card"
          v-for="item in visibleSchedules"
          :key="item.scheduleId"
        >
          <view class="row"
            ><text class="schedule-date">{{ shortDate(item.startTime) }}</text
            ><text
              class="tag"
              :class="{ full: Number(item.availableCount) <= 0 }"
              >{{
                item.availableCount == null
                  ? "名额待确认"
                  : Number(item.availableCount) > 0
                    ? "剩余 " + item.availableCount + " 位"
                    : "已满员"
              }}</text
            ></view
          >
          <view class="time-range"
            >{{ formatTime(item.startTime) }}<text>—</text
            >{{ formatTime(item.endTime) }}</view
          >
          <view class="card-title schedule-name" @tap="openCourse(item)"
            >{{ item.courseName }} <text class="inline-arrow">›</text></view
          >
          <view class="card-desc"
            >{{ item.coachName || "教练待安排" }}<text class="dot">·</text
            >{{ item.storeName || "门店待定" }}</view
          >
          <view
            class="booking-window"
            v-if="item.reservationOpenTime || item.reservationDeadline"
          >
            <view v-if="item.reservationOpenTime"
              >开放预约 {{ formatDateTime(item.reservationOpenTime) }}</view
            >
            <view v-if="item.reservationDeadline"
              >预约截止 {{ formatDateTime(item.reservationDeadline) }}</view
            >
          </view>
          <view class="card-footer"
            ><view
              ><text class="price">{{ priceText(item.displayPrice) }}</text
              ><text class="price-caption">本场参考价 · 线下咨询</text></view
            ><button
              class="btn"
              :disabled="
                item.reservedByMe ||
                Number(item.availableCount) <= 0 ||
                isActioning('reserve', item.scheduleId)
              "
              @tap="reserveSchedule(item)"
            >
              {{
                item.reservedByMe
                  ? "已预约"
                  : isActioning("reserve", item.scheduleId)
                    ? "预约中…"
                    : "预约本场"
              }}
            </button></view
          >
        </view>
        <view v-if="!visibleSchedules.length" class="empty"
          ><text class="empty-title">这一天还没有可约场次</text
          >试试其他日期，或联系教练安排。</view
        >
        <view class="batch-toggle row" @tap="toggleBatchDateEditor"
          ><view
            ><view class="section-title">一次预约多个时段</view
            ><view class="muted">已有固定训练时间时使用</view></view
          ><text class="text-link">{{
            batchDateEditorVisible ? "收起 −" : "展开 +"
          }}</text></view
        >
        <view v-if="batchDateEditorVisible" class="panel batch-editor">
          <view class="muted"
            >按当前课表填写日期与时间，可选填门店、课程和教练缩小范围。</view
          >
          <view class="batch-filter-row">
            <input
              class="batch-input"
              v-model="batchFilters.storeName"
              placeholder="门店名称（选填）"
              placeholder-style="color:#7d8077"
            />
            <input
              class="batch-input"
              v-model="batchFilters.courseName"
              placeholder="课程名称（选填）"
              placeholder-style="color:#7d8077"
            />
            <input
              class="batch-input"
              v-model="batchFilters.coachName"
              placeholder="教练姓名（选填）"
              placeholder-style="color:#7d8077"
            />
          </view>
          <view
            class="batch-row"
            v-for="(row, idx) in batchDateRows"
            :key="'row-' + idx"
          >
            <view class="row"
              ><text class="muted">时段 {{ idx + 1 }}</text
              ><text class="row-del" @tap="removeBatchRow(idx)"
                >移除</text
              ></view
            >
            <picker
              mode="date"
              :value="row.scheduleDate"
              @change="updateBatchRowDate(idx, $event.detail.value)"
              ><view class="picker-box"
                >{{ row.scheduleDate || "选择训练日期" }}<text>›</text></view
              ></picker
            >
            <view class="time-pickers">
              <picker
                mode="time"
                :value="row.startTime"
                @change="updateBatchRowStart(idx, $event.detail.value)"
                ><view class="picker-box">{{
                  row.startTime || "开始时间"
                }}</view></picker
              >
              <text class="muted">—</text>
              <picker
                mode="time"
                :value="row.endTime"
                @change="updateBatchRowEnd(idx, $event.detail.value)"
                ><view class="picker-box">{{
                  row.endTime || "结束时间"
                }}</view></picker
              >
            </view>
          </view>
          <button class="btn ghost block-btn" @tap="addBatchRow">
            ＋ 添加时段
          </button>
          <button
            class="btn block-btn"
            :disabled="isActioning('batchReserve', 0)"
            @tap="batchReserveByDateRows"
          >
            {{ isActioning("batchReserve", 0) ? "预约中…" : "确认这些预约" }}
          </button>
        </view>
      </block>
      <block v-if="currentTab === 'reservation'">
        <view
          class="card"
          v-for="item in reservationList"
          :key="item.reservationId"
        >
          <view class="row"
            ><text class="schedule-date">{{ shortDate(item.startTime) }}</text
            ><text class="tag">{{
              reservationText(item.reservationStatus)
            }}</text></view
          >
          <view class="time-range"
            >{{ formatTime(item.startTime) }}<text>—</text
            >{{ formatTime(item.endTime) }}</view
          >
          <view class="card-title">{{ item.courseName }}</view>
          <view class="card-desc"
            >{{ item.coachName || "教练待安排" }}<text class="dot">·</text
            >{{ item.storeName || "门店待定" }}</view
          >
          <view class="checkin-line"
            ><text class="status-dot"></text
            >{{ checkinText(item.derivedCheckinStatus) }}</view
          >
          <view class="actions" v-if="item.reservationStatus === 'reserved'">
            <button
              class="btn ghost"
              :disabled="isActioning('cancel', item.reservationId)"
              @tap="cancelReservation(item)"
            >
              {{
                isActioning("cancel", item.reservationId)
                  ? "取消中…"
                  : "取消预约"
              }}
            </button>
            <button
              class="btn"
              :disabled="isActioning('checkin', item.reservationId)"
              @tap="memberCheckin(item)"
            >
              {{
                isActioning("checkin", item.reservationId)
                  ? "签到中…"
                  : "到场签到"
              }}
            </button>
          </view>
        </view>
        <view v-if="!reservationList.length" class="empty"
          ><text class="empty-title">下一次训练，从预约开始</text
          >挑一节喜欢的课，为自己留一点时间。<button
            class="btn empty-action"
            @tap="switchTab('schedule')"
          >
            去选时段
          </button></view
        >
      </block>
      <block v-if="currentTab === 'card'">
        <view
          class="card membership-card"
          v-for="item in cardList"
          :key="item.cardId"
        >
          <view class="row"
            ><text class="eyebrow">T1 MEMBERSHIP</text
            ><text class="tag">{{
              cardStatusText(item.cardStatus)
            }}</text></view
          >
          <view class="card-title">{{ item.cardName }}</view>
          <view class="card-desc">{{
            item.coachName ? "专属教练 · " + item.coachName : "尚未绑定教练"
          }}</view>
          <view class="balance-row"
            ><view
              ><text class="balance">{{
                remainTimesText(item.remainTimes)
              }}</text
              ><text class="price-caption">剩余次数</text></view
            ><view
              ><text class="balance">{{
                item.balanceAmount == null
                  ? "—"
                  : "¥" + formatAmount(item.balanceAmount)
              }}</text
              ><text class="price-caption">卡内余额</text></view
            ></view
          >
          <view class="muted" v-if="item.expireTime"
            >有效至 {{ formatDateOnly(item.expireTime) }}</view
          >
        </view>
        <view v-if="!cardList.length" class="empty"
          ><text class="empty-title">还没有会员卡</text
          >想了解适合的方案，可以到店与教练聊聊。</view
        >
      </block>
      <block v-if="currentTab === 'order'">
        <view class="card" v-for="item in orderList" :key="item.orderId">
          <view class="row"
            ><text class="section-title">{{
              item.storeName || "门店办理"
            }}</text
            ><text class="tag">{{
              orderStatusText(item.orderStatus)
            }}</text></view
          >
          <view class="card-desc">记录编号 {{ item.orderNo }}</view>
          <view class="card-footer"
            ><text class="muted">实收 {{ priceText(item.paidAmount) }}</text
            ><text class="muted"
              >应收 {{ priceText(item.payableAmount) }}</text
            ></view
          >
        </view>
        <view v-if="!orderList.length" class="empty"
          ><text class="empty-title">暂无线下办理记录</text
          >在门店办理的会员卡记录会显示在这里。</view
        >
      </block>
    </block>
    <T1Nav role="member" current="course" />
  </view>
</template>
<script>
import T1Nav from "../../components/T1Nav.vue";
import {
  memberCourseList,
  memberScheduleList,
  memberCreateReservation,
  memberReservationList,
  memberCancelReservation,
  memberCreateCheckin,
  memberMyCards,
  memberMyOrders,
} from "../../api/auth";

export default {
  components: { T1Nav },
  data() {
    return {
      tabs: [
        { label: "课程", value: "course" },
        { label: "选时段", value: "schedule" },
        { label: "我的预约", value: "reservation" },
        { label: "会员卡", value: "card" },
        { label: "线下记录", value: "order" },
      ],
      currentTab: "course",
      courseType: "",
      selectedDate: "",
      selectedCourseId: "",
      selectedCourseName: "",
      courseList: [],
      scheduleList: [],
      reservationList: [],
      cardList: [],
      orderList: [],
      loading: false,
      errorText: "",
      actioningKey: "",
      batchDateEditorVisible: false,
      batchFilters: {
        storeName: "",
        courseName: "",
        coachName: "",
      },
      batchDateRows: [{ scheduleDate: "", startTime: "", endTime: "" }],
    };
  },
  computed: {
    scheduleDates() {
      return [
        ...new Set(
          this.scheduleList
            .map((item) => this.formatDateOnly(item.startTime))
            .filter(Boolean),
        ),
      ];
    },
    visibleSchedules() {
      return this.selectedDate
        ? this.scheduleList.filter(
            (item) => this.formatDateOnly(item.startTime) === this.selectedDate,
          )
        : this.scheduleList;
    },
    sectionLabel() {
      return {
        course: "找到适合你的训练",
        schedule: "按日期选择一节课",
        reservation: "每一次到场，都算数",
        card: "你的会员卡",
        order: "线下办理记录",
      }[this.currentTab];
    },
  },
  onLoad(options) {
    const tab = options && options.tab;
    this.selectedCourseId = (options && options.courseId) || "";
    try {
      this.selectedCourseName = decodeURIComponent(
        (options && options.courseName) || "",
      );
    } catch (_) {
      this.selectedCourseName = (options && options.courseName) || "";
    }
    if (
      ["course", "schedule", "reservation", "card", "order", "plan"].includes(
        tab,
      )
    ) {
      this.currentTab = tab === "plan" ? "card" : tab;
    }
  },
  onShow() {
    this.loadCurrentTabData();
  },
  methods: {
    displayDate(value) {
      let normalized = value;
      if (
        typeof value === "string" &&
        /^\d{4}-\d{2}-\d{2}(?:[ T]\d{2}:\d{2}(?::\d{2})?)?$/.test(value)
      )
        normalized =
          value.length === 10
            ? value + "T00:00:00+08:00"
            : value.replace(" ", "T") + "+08:00";
      const date = new Date(normalized);
      return new Date(date.getTime() + 8 * 60 * 60 * 1000);
    },
    clearCourseFilter() {
      this.selectedCourseId = "";
      this.selectedCourseName = "";
      this.selectedDate = "";
      this.loadCurrentTabData();
    },
    courseTypeText(value) {
      return (
        {
          group: "团体课",
          private: "私教课",
          small: "小班课",
          experience: "体验课",
        }[value] || "训练课程"
      );
    },
    priceText(value) {
      return value === null || value === undefined || value === ""
        ? "价格待咨询"
        : "¥" + this.formatAmount(value);
    },
    shortDate(value) {
      const date = this.formatDateOnly(value);
      return date ? date.slice(5).replace("-", " / ") : "日期待定";
    },
    cardStatusText(value) {
      return (
        {
          unused: "待启用",
          normal: "可使用",
          active: "可使用",
          expired: "已到期",
          frozen: "已冻结",
          disabled: "已停用",
          used_up: "已用完",
          0: "可使用",
          1: "已停用",
        }[value] || "查看权益"
      );
    },
    orderStatusText(value) {
      return (
        {
          paid: "已办理",
          completed: "已完成",
          pending: "待确认",
          unpaid: "待确认",
          cancelled: "已取消",
          refunded: "已退款",
        }[value] || "办理记录"
      );
    },

    openCourse(item) {
      uni.navigateTo({ url: "/pages/course/detail?id=" + item.courseId });
    },
    switchTab(tab) {
      if (this.currentTab === tab || this.loading) {
        return;
      }
      this.currentTab = tab;
      this.loadCurrentTabData();
    },
    async loadCurrentTabData() {
      if (this.loading) {
        return;
      }
      this.loading = true;
      this.errorText = "";
      try {
        if (this.currentTab === "course") {
          this.courseList =
            (await memberCourseList({ courseType: this.courseType })) || [];
          return;
        }
        if (this.currentTab === "schedule") {
          const table =
            (await memberScheduleList({
              pageNum: 1,
              pageSize: 20,
              courseId: this.selectedCourseId || undefined,
            })) || {};
          this.scheduleList = table.rows || [];
          return;
        }
        if (this.currentTab === "reservation") {
          const table =
            (await memberReservationList({ pageNum: 1, pageSize: 20 })) || {};
          this.reservationList = table.rows || [];
          return;
        }
        if (this.currentTab === "card") {
          this.cardList = (await memberMyCards()) || [];
          return;
        }
        if (this.currentTab === "order") {
          const table =
            (await memberMyOrders({ pageNum: 1, pageSize: 20 })) || {};
          this.orderList = table.rows || [];
        }
      } catch (error) {
        this.errorText = error.message || "课程暂时没能加载";
        uni.showToast({ title: this.errorText, icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    async changeCourseType(value) {
      if (this.courseType === value || this.loading) {
        return;
      }
      this.courseType = value;
      await this.loadCurrentTabData();
    },
    buildActionKey(type, id) {
      return `${type}-${id}`;
    },
    isActioning(type, id) {
      return this.actioningKey === this.buildActionKey(type, id);
    },
    setActioning(type, id) {
      this.actioningKey = this.buildActionKey(type, id);
    },
    clearActioning() {
      this.actioningKey = "";
    },
    confirmAction(content) {
      return new Promise((resolve) => {
        uni.showModal({
          title: "请确认",
          content,
          success: (res) => resolve(!!res.confirm),
          fail: () => resolve(false),
        });
      });
    },
    toggleBatchDateEditor() {
      this.batchDateEditorVisible = !this.batchDateEditorVisible;
    },
    addBatchRow() {
      this.batchDateRows.push({ scheduleDate: "", startTime: "", endTime: "" });
    },
    removeBatchRow(index) {
      if (this.batchDateRows.length <= 1) {
        uni.showToast({ title: "至少保留1行", icon: "none" });
        return;
      }
      this.batchDateRows.splice(index, 1);
    },
    updateBatchRowDate(index, value) {
      this.batchDateRows[index].scheduleDate = value;
    },
    updateBatchRowStart(index, value) {
      this.batchDateRows[index].startTime = value;
    },
    updateBatchRowEnd(index, value) {
      this.batchDateRows[index].endTime = value;
    },
    normalizeText(value) {
      return (value || "").toString().trim().toLowerCase();
    },
    matchBatchFilter(item) {
      const storeFilter = this.normalizeText(this.batchFilters.storeName);
      const courseFilter = this.normalizeText(this.batchFilters.courseName);
      const coachFilter = this.normalizeText(this.batchFilters.coachName);
      const storeName = this.normalizeText(item.storeName);
      const courseName = this.normalizeText(item.courseName);
      const coachName = this.normalizeText(item.coachName);
      if (storeFilter && !storeName.includes(storeFilter)) {
        return false;
      }
      if (courseFilter && !courseName.includes(courseFilter)) {
        return false;
      }
      if (coachFilter && !coachName.includes(coachFilter)) {
        return false;
      }
      return true;
    },
    findScheduleForBatchRow(row) {
      if (!row || !row.scheduleDate || !row.startTime || !row.endTime) {
        return null;
      }
      return (
        this.scheduleList.find((item) => {
          if (
            !item ||
            !item.scheduleId ||
            item.reservedByMe ||
            !this.matchBatchFilter(item)
          ) {
            return false;
          }
          const startDate = this.formatDateOnly(item.startTime);
          const startClock = this.formatTime(item.startTime);
          const endClock = this.formatTime(item.endTime);
          return (
            startDate === row.scheduleDate &&
            startClock === row.startTime &&
            endClock === row.endTime
          );
        }) || null
      );
    },
    formatDateOnly(value) {
      if (!value) {
        return "";
      }
      const date = this.displayDate(value);
      if (Number.isNaN(date.getTime())) {
        return "";
      }
      const y = date.getUTCFullYear();
      const m = `${date.getUTCMonth() + 1}`.padStart(2, "0");
      const d = `${date.getUTCDate()}`.padStart(2, "0");
      return `${y}-${m}-${d}`;
    },
    async buildReservationPayload(item, cardCache = {}) {
      const payload = { scheduleId: item.scheduleId };
      if (item.courseType === "private" || item.courseType === "small") {
        const coachKey = `${item.storeId || "none"}:${item.coachId || "none"}`;
        let cardId = cardCache[coachKey];
        if (!cardId) {
          cardId = await this.pickReservationCard(item);
          if (!cardId) {
            throw new Error("已取消选择，剩余课程未预约");
          }
          cardCache[coachKey] = cardId;
        }
        if (cardId !== "without-card") payload.cardId = cardId;
      }
      return payload;
    },
    async batchReserveByDateRows() {
      if (this.actioningKey || !this.batchDateRows.length) {
        return;
      }
      const invalidIndex = this.batchDateRows.findIndex(
        (row) => !row.scheduleDate || !row.startTime || !row.endTime,
      );
      if (invalidIndex >= 0) {
        uni.showToast({
          title: `第${invalidIndex + 1}行未填写完整`,
          icon: "none",
        });
        return;
      }
      const matched = [];
      const unmatched = [];
      this.batchDateRows.forEach((row, idx) => {
        const found = this.findScheduleForBatchRow(row);
        if (found) {
          matched.push(found);
        } else {
          unmatched.push(idx + 1);
        }
      });
      if (!matched.length) {
        uni.showToast({ title: "未匹配到可预约排课", icon: "none" });
        return;
      }
      if (unmatched.length) {
        uni.showToast({
          title: `第${unmatched.join(",")}行未匹配到排课`,
          icon: "none",
        });
        return;
      }
      const uniqueSchedules = Array.from(
        new Map(matched.map((item) => [item.scheduleId, item])).values(),
      );
      const confirmed = await this.confirmAction(
        `确认批量预约 ${uniqueSchedules.length} 条排课吗？`,
      );
      if (!confirmed) {
        return;
      }
      this.setActioning("batchReserve", 0);
      let success = 0;
      try {
        uni.showLoading({ title: "批量预约中..." });
        const cardCache = {};
        for (const item of uniqueSchedules) {
          const payload = await this.buildReservationPayload(item, cardCache);
          await memberCreateReservation(payload);
          success += 1;
        }
        uni.showToast({ title: `批量预约成功 ${success} 条`, icon: "success" });
        await Promise.all([
          this.currentTab === "schedule"
            ? this.loadCurrentTabData()
            : Promise.resolve(),
          this.refreshReservations(),
        ]);
      } catch (error) {
        uni.showToast({
          title: error.message || `批量预约失败（成功${success}条）`,
          icon: "none",
        });
      } finally {
        this.clearActioning();
        uni.hideLoading();
      }
    },
    async reserveSchedule(item) {
      if (!item || !item.scheduleId || this.actioningKey) {
        return;
      }
      this.setActioning("reserve", item.scheduleId);
      try {
        uni.showLoading({ title: "预约中..." });
        const payload = { scheduleId: item.scheduleId };
        if (item.courseType === "private" || item.courseType === "small") {
          const cardId = await this.pickReservationCard(item);
          if (!cardId) {
            return;
          }
          if (cardId !== "without-card") payload.cardId = cardId;
        }
        await memberCreateReservation(payload);
        uni.showToast({ title: "预约成功", icon: "success" });
        await Promise.all([
          this.currentTab === "schedule"
            ? this.loadCurrentTabData()
            : Promise.resolve(),
          this.refreshReservations(),
        ]);
      } catch (error) {
        uni.showToast({ title: error.message || "预约失败", icon: "none" });
      } finally {
        this.clearActioning();
        uni.hideLoading();
      }
    },
    async pickReservationCard(scheduleItem) {
      const cards = (await memberMyCards()) || [];
      const availableCards = cards.filter((card) => {
        if (!card || !card.cardId) {
          return false;
        }
        if (card.planType === "time_card" || card.planType === "stored_card") {
          return false;
        }
        if (
          card.bindCoachId &&
          scheduleItem.coachId &&
          card.bindCoachId !== scheduleItem.coachId
        ) {
          return false;
        }
        return true;
      });
      uni.hideLoading();
      return new Promise((resolve) => {
        uni.showActionSheet({
          itemList: [
            "无卡预约 · 线下咨询",
            ...availableCards.map(
              (card) =>
                `${card.cardName || "卡项"}（${card.coachName || "未绑定教练"}）`,
            ),
          ],
          success: (res) =>
            resolve(
              res.tapIndex === 0
                ? "without-card"
                : availableCards[res.tapIndex - 1].cardId,
            ),
          fail: () => resolve(null),
        });
      });
    },

    async cancelReservation(item) {
      if (!item || !item.reservationId || this.actioningKey) {
        return;
      }
      const confirmed = await this.confirmAction("确认取消本次预约吗？");
      if (!confirmed) {
        return;
      }
      this.setActioning("cancel", item.reservationId);
      try {
        uni.showLoading({ title: "取消中..." });
        await memberCancelReservation(item.reservationId);
        uni.showToast({ title: "已取消预约", icon: "success" });
        await this.refreshReservations();
      } catch (error) {
        uni.showToast({ title: error.message || "取消失败", icon: "none" });
      } finally {
        this.clearActioning();
        uni.hideLoading();
      }
    },
    async memberCheckin(item) {
      if (!item || !item.reservationId || this.actioningKey) {
        return;
      }
      this.setActioning("checkin", item.reservationId);
      try {
        uni.showLoading({ title: "签到中..." });
        await memberCreateCheckin({
          reservationId: item.reservationId,
          cardId: item.cardId,
        });
        uni.showToast({ title: "签到成功，待教练核销", icon: "success" });
        await this.refreshReservations();
      } catch (error) {
        uni.showToast({ title: error.message || "签到失败", icon: "none" });
      } finally {
        this.clearActioning();
        uni.hideLoading();
      }
    },
    async refreshReservations() {
      const table =
        (await memberReservationList({ pageNum: 1, pageSize: 20 })) || {};
      this.reservationList = table.rows || [];
      if (this.currentTab === "schedule") {
        const scheduleTable =
          (await memberScheduleList({
            pageNum: 1,
            pageSize: 20,
            courseId: this.selectedCourseId || undefined,
          })) || {};
        this.scheduleList = scheduleTable.rows || [];
      }
    },
    reservationText(value) {
      const map = {
        reserved: "已预约",
        cancelled: "已取消",
        signed: "已签到",
        absent: "缺席",
        leave: "请假",
      };
      return map[value] || value || "未知";
    },
    checkinText(value) {
      const map = {
        not_started: "未签到",
        pending_confirm: "待核销",
        confirmed: "已核销",
        exception: "异常",
      };
      return map[value] || value || "未开始";
    },
    formatDateTime(value) {
      if (!value) {
        return "待定";
      }
      const date = this.displayDate(value);
      if (Number.isNaN(date.getTime())) {
        return "待定";
      }
      return `${date.getUTCMonth() + 1}/${date.getUTCDate()} ${this.formatTime(value)}`;
    },
    formatTime(value) {
      if (!value) {
        return "--:--";
      }
      const date = this.displayDate(value);
      if (Number.isNaN(date.getTime())) {
        return "--:--";
      }
      const hh = `${date.getUTCHours()}`.padStart(2, "0");
      const mm = `${date.getUTCMinutes()}`.padStart(2, "0");
      return `${hh}:${mm}`;
    },
    formatAmount(value) {
      return Number(value || 0).toFixed(2);
    },
    remainTimesText(value) {
      if (value === 0 || !!value) {
        return value;
      }
      return "--";
    },
  },
};
</script>
<style scoped>
.page-wrap {
  min-height: 100vh;
  box-sizing: border-box;
  background: #111211;
  color: #f6f4ef;
  padding: 32rpx 32rpx calc(48rpx + env(safe-area-inset-bottom));
  max-width: 960px;
  margin: auto;
}
.with-nav {
  padding-bottom: calc(164rpx + env(safe-area-inset-bottom));
}
.eyebrow {
  font-size: 20rpx;
  letter-spacing: 4rpx;
  color: #ff7a35;
  font-weight: 700;
}
.page-title {
  font-size: 52rpx;
  line-height: 1.25;
  font-weight: 700;
  margin-top: 12rpx;
  letter-spacing: -1rpx;
}
.page-subtitle,
.muted {
  font-size: 25rpx;
  line-height: 1.7;
  color: #a2a49c;
}
.page-subtitle {
  margin-top: 14rpx;
}
.panel,
.card {
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-top: 24rpx;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: 700;
}
.tag {
  font-size: 21rpx;
  line-height: 1.5;
  padding: 8rpx 14rpx;
  background: #30271f;
  color: #ff7a35;
  border-radius: 8rpx;
  white-space: nowrap;
  flex-shrink: 0;
}
.btn {
  margin: 0;
  min-height: 88rpx;
  line-height: 88rpx;
  padding: 0 28rpx;
  border-radius: 14rpx;
  background: #ff7a35;
  color: #111211;
  font-size: 28rpx;
  font-weight: 700;
}
.btn::after {
  border: 0;
}
.btn.ghost {
  background: transparent;
  border: 1rpx solid #42453f;
  color: #f6f4ef;
}
.btn[disabled] {
  background: #30322f;
  color: #85887f;
  opacity: 0.7;
}
.empty {
  padding: 64rpx 28rpx;
  text-align: center;
  border: 1rpx dashed #30322f;
  border-radius: 20rpx;
  margin-top: 24rpx;
  color: #a2a49c;
  font-size: 26rpx;
  line-height: 1.8;
}
.empty-title {
  display: block;
  color: #f6f4ef;
  font-size: 32rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
}
.error {
  color: #ffb18c;
}
.scroll {
  white-space: nowrap;
  width: 100%;
  margin-top: 28rpx;
}
.chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 80rpx;
  padding: 0 26rpx;
  margin-right: 12rpx;
  border-radius: 14rpx;
  border: 1rpx solid #30322f;
  background: #1c1d1b;
  color: #a2a49c;
  font-size: 26rpx;
  box-sizing: border-box;
}
.chip.active {
  color: #111211;
  background: #ff7a35;
  border-color: #ff7a35;
  font-weight: 700;
}
.back-link {
  display: inline-flex;
  align-items: center;
  min-height: 80rpx;
  color: #a2a49c;
  font-size: 26rpx;
  margin: -12rpx 0 12rpx;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
  margin-top: 40rpx;
}
.course-filter {
  font-size: 24rpx;
  color: #a2a49c;
  border-bottom: 1rpx solid #30322f;
  margin-top: 12rpx;
}
.clear-filter {
  color: #ff7a35;
  padding: 24rpx 0 24rpx 20rpx;
  white-space: nowrap;
}
.filters {
  margin-top: 20rpx;
}
.filters .chip {
  min-height: 72rpx;
  background: transparent;
}
.filters .chip.active {
  background: #30271f;
  color: #ff7a35;
  border-color: #ff7a35;
}
.course-cover {
  display: block;
  width: 100%;
  height: 300rpx;
  border-radius: 14rpx;
  margin-bottom: 24rpx;
}
.card-title {
  font-size: 35rpx;
  line-height: 1.45;
  font-weight: 700;
  margin-top: 20rpx;
  word-break: break-all;
}
.card-desc {
  font-size: 25rpx;
  color: #a2a49c;
  line-height: 1.8;
  margin-top: 12rpx;
}
.dot {
  padding: 0 12rpx;
}
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  border-top: 1rpx solid #30322f;
  padding-top: 24rpx;
  margin-top: 24rpx;
}
.price {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #ff7a35;
}
.price-caption {
  display: block;
  font-size: 21rpx;
  color: #a2a49c;
  margin-top: 7rpx;
  line-height: 1.5;
}
.text-link {
  font-size: 25rpx;
  color: #ff7a35;
  white-space: nowrap;
}
.date-chip {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  min-width: 126rpx;
  height: 124rpx;
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 16rpx;
  margin-right: 14rpx;
  padding: 0 18rpx;
  font-size: 21rpx;
  color: #a2a49c;
}
.date-chip.active {
  background: #ff7a35;
  color: #111211;
  border-color: #ff7a35;
}
.date-day {
  font-size: 29rpx;
  font-weight: 700;
}
.schedule-date {
  font-size: 27rpx;
  color: #ff7a35;
  font-weight: 600;
}
.time-range {
  font-size: 44rpx;
  line-height: 1.3;
  font-weight: 700;
  margin-top: 20rpx;
  font-variant-numeric: tabular-nums;
}
.time-range text {
  padding: 0 16rpx;
  color: #70756a;
  font-weight: 400;
  font-size: 30rpx;
}
.schedule-name {
  margin-top: 24rpx;
  font-size: 31rpx;
}
.inline-arrow {
  color: #a2a49c;
  margin-left: 8rpx;
}
.booking-window {
  font-size: 22rpx;
  line-height: 1.8;
  margin-top: 20rpx;
  color: #a2a49c;
}
.tag.full {
  background: #30322f;
  color: #a2a49c;
}
.actions {
  display: flex;
  gap: 18rpx;
  margin-top: 24rpx;
}
.actions .btn {
  flex: 1;
}
.checkin-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 24rpx;
  color: #a2a49c;
  font-size: 25rpx;
}
.status-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #ff7a35;
}
.empty-action {
  margin: 28rpx auto 0;
  max-width: 320rpx;
}
.batch-toggle {
  margin-top: 40rpx;
  padding: 24rpx 0;
  border-top: 1rpx solid #30322f;
}
.batch-toggle .section-title {
  font-size: 27rpx;
}
.batch-toggle .muted {
  font-size: 22rpx;
  margin-top: 8rpx;
}
.batch-filter-row {
  margin-top: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}
.batch-input,
.picker-box {
  background: #111211;
  border: 1rpx solid #30322f;
  border-radius: 12rpx;
  box-sizing: border-box;
  min-height: 88rpx;
  padding: 0 20rpx;
  font-size: 26rpx;
  color: #f6f4ef;
}
.batch-input {
  height: 88rpx;
}
.picker-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.batch-row {
  border-top: 1rpx solid #30322f;
  margin-top: 24rpx;
  padding-top: 16rpx;
}
.row-del {
  color: #a2a49c;
  font-size: 24rpx;
  padding: 18rpx 0 18rpx 24rpx;
}
.time-pickers {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 14rpx;
}
.time-pickers picker {
  flex: 1;
}
.time-pickers .picker-box {
  justify-content: center;
}
.block-btn {
  margin-top: 20rpx;
}
.balance-row {
  display: flex;
  gap: 56rpx;
  border-top: 1rpx solid #30322f;
  margin: 28rpx 0;
  padding-top: 28rpx;
}
.balance {
  font-size: 40rpx;
  font-weight: 700;
  color: #f6f4ef;
}
.retry {
  margin-top: 16rpx;
  color: #ff7a35;
}
</style>
