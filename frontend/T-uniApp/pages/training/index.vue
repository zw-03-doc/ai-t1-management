<template>
  <view class="training-page">
    <view class="hero">
      <text class="eyebrow">T1 / TRAINING JOURNAL</text>
      <view class="heading">{{
        role === "coach" ? "为学员定制训练" : "我的训练计划"
      }}</view>
      <view class="muted"
        >让每一次训练有迹可循。计划目标与实际记录分别保存，热身与正式组独立统计。</view
      >
    </view>
    <view v-if="error" class="error" @tap="load">{{ error }} · 点击重试</view>
    <view v-if="loading" class="card">正在读取计划…</view>
    <view v-if="!selected && !editor" class="card">
      <view class="section-title">免费计划库</view>
      <view
        v-for="t in templates"
        :key="t.templateId || t.planId"
        class="template-row"
      >
        <view
          ><view class="name">{{ t.name }}</view
          ><text class="muted">{{ t.cycleDays }} 天一个循环 · 免费</text></view
        >
        <button size="mini" @tap="copyTemplate(t)">按此定制</button>
      </view>
      <button class="primary" @tap="newPlan">从空白计划开始</button>
    </view>
    <view v-if="!selected && !editor" class="card">
      <view class="section-title">{{
        role === "coach" ? "学员的计划" : "我的计划"
      }}</view>
      <view v-if="!plans.length" class="muted"
        >还没有计划，选一个免费模板开始吧。</view
      >
      <view
        v-for="p in plans"
        :key="p.planId"
        class="template-row"
        @tap="openPlan(p)"
      >
        <view
          ><view class="name">{{ p.name }}</view
          ><text class="muted"
            >{{ p.cycleDays }} 天循环 ·
            {{ p.status === "active" ? "进行中" : "未开启" }}</text
          ></view
        ><text>查看 →</text>
      </view>
    </view>
    <view v-if="editor" class="card">
      <view class="section-title">定制训练表</view>
      <text class="label">计划名称</text
      ><input v-model="editor.name" placeholder="例如：我的三分化训练" />
      <text class="label">计划说明</text
      ><textarea
        v-model="editor.description"
        placeholder="训练目标与注意事项"
      />
      <view class="row"
        ><text>循环总天数（含休息日）</text
        ><input
          class="small"
          type="number"
          :disabled="editor.status === 'active'"
          v-model.number="editor.cycleDays"
          @blur="resizeDays"
      /></view>
      <view class="row"
        ><text>公开免费分享</text
        ><switch
          :checked="editor.visibility === 'public'"
          @change="
            editor.visibility = $event.detail.value ? 'public' : 'private'
          "
      /></view>
      <view class="muted"
        >公开只分享计划动作与目标，不分享你的实际训练记录。</view
      >
      <view class="day-tabs"
        ><text
          v-for="(day, i) in editor.days"
          :key="day.dayNumber"
          :class="['day-tab', { active: dayIndex === i }]"
          @tap="dayIndex = i"
          >第 {{ day.dayNumber }} 天</text
        ></view
      >
      <view v-if="currentDay">
        <input
          v-model="currentDay.title"
          placeholder="当天名称，如推 / 拉 / 腿"
        />
        <view class="row"
          ><text>休息日</text
          ><switch :checked="currentDay.restDay" @change="toggleRest"
        /></view>
        <view v-if="!currentDay.restDay">
          <view
            v-for="(ex, ei) in currentDay.exercises"
            :key="ex.key"
            class="exercise"
          >
            <view class="row"
              ><input v-model="ex.name" placeholder="动作名称" /><text
                class="danger"
                @tap="currentDay.exercises.splice(ei, 1)"
                >删除</text
              ></view
            >
            <textarea
              v-model="ex.instructions"
              placeholder="动作要领、节奏、休息时间"
            />
            <view class="muted">动作图片与短视频</view>
            <view v-if="editor.planId" class="actions"
              ><button size="mini" :disabled="busy" @tap="upload(ex, 'image')">
                上传图片</button
              ><button size="mini" :disabled="busy" @tap="upload(ex, 'video')">
                上传短视频
              </button></view
            >
            <view v-else class="muted"
              >先保存计划，再进入编辑即可上传图片和短视频。</view
            >
            <view v-for="(url, ui) in ex.imageUrls" :key="ui" class="row"
              ><input
                v-model="ex.imageUrls[ui]"
                placeholder="https:// 图片地址"
              /><text @tap="ex.imageUrls.splice(ui, 1)">×</text></view
            >
            <button size="mini" @tap="ex.imageUrls.push('')">
              添加图片链接
            </button>
            <input
              v-model="ex.videoUrl"
              placeholder="短视频 https:// 地址（MP4）"
            />
            <view class="set-head">组别 / 次数 / 重量 / 单位 / 强度 RPE</view>
            <view v-for="(set, si) in ex.targets" :key="si" class="set-row">
              <picker
                :range="['热身', '正式']"
                :value="set.setType === 'warmup' ? 0 : 1"
                @change="
                  set.setType =
                    Number($event.detail.value) === 0 ? 'warmup' : 'working'
                "
                ><view class="pill">{{
                  set.setType === "warmup" ? "热身" : "正式"
                }}</view></picker
              >
              <input
                type="number"
                v-model.number="set.reps"
                placeholder="次数"
              /><input
                type="digit"
                v-model.number="set.weight"
                placeholder="重量"
              />
              <picker
                :range="['kg', 'lb']"
                :value="set.unit === 'kg' ? 0 : 1"
                @change="
                  set.unit = Number($event.detail.value) === 0 ? 'kg' : 'lb'
                "
                ><view class="pill">{{ set.unit }}</view></picker
              >
              <input
                type="digit"
                v-model.number="set.rpe"
                placeholder="RPE"
              /><text @tap="ex.targets.splice(si, 1)">×</text>
            </view>
            <button size="mini" @tap="ex.targets.push(emptySet())">
              添加一组
            </button>
          </view>
          <button @tap="addExercise">添加训练动作</button>
        </view>
      </view>
      <view class="actions"
        ><button :disabled="busy" @tap="editor = null">取消</button
        ><button
          class="primary"
          :loading="busy"
          :disabled="busy"
          @tap="savePlan"
        >
          保存计划
        </button></view
      >
    </view>
    <view v-if="selected && !editor">
      <view class="card">
        <view class="row"
          ><view class="section-title">{{ selected.name }}</view
          ><text @tap="selected = null">返回列表</text></view
        >
        <view class="muted"
          >{{ selected.description }} · 每 {{ selected.cycleDays }} 天循环</view
        >
        <button size="mini" @tap="editSelected">编辑后续动作与目标</button
        ><view class="muted"
          >修改只影响未完成日程，已完成训练的动作与重量记录保留。</view
        >
        <view v-if="selected.status !== 'active'" class="start-form">
          <view class="row"
            ><text>开始日期（北京时间）</text
            ><picker
              mode="date"
              :value="startDate"
              @change="startDate = $event.detail.value"
              ><view class="pill">{{ startDate }}</view></picker
            ></view
          >
          <view class="row"
            ><text>循环次数</text
            ><input class="small" type="number" v-model.number="cycles"
          /></view>
          <button class="primary" :disabled="busy" @tap="startPlan">
            开启计划
          </button>
        </view>
      </view>
      <view v-if="workouts.length" class="card">
        <view class="section-title">训练日程</view
        ><view class="muted"
          >临时有事时，可将当天及后续未完成训练一起顺延一天，已完成记录不变。</view
        >
        <view v-for="w in workouts" :key="w.workoutId" class="workout">
          <view class="row"
            ><view
              ><view class="name">{{ w.plannedDate }} · {{ w.title }}</view
              ><text class="muted"
                >{{
                  w.status === "completed"
                    ? "已完成"
                    : w.status === "rest"
                      ? "休息日"
                      : "待训练"
                }}<text v-if="w.originalDate !== w.plannedDate">
                  · 原定 {{ w.originalDate }}</text
                ></text
              ></view
            ></view
          >
          <view v-if="w.status !== 'rest'" class="actions"
            ><button size="mini" @tap="openLog(w)">
              {{
                w.status === "completed" ? "查看 / 修改记录" : "记录本次训练"
              }}</button
            ><button
              v-if="w.status !== 'completed'"
              size="mini"
              :disabled="busy"
              @tap="shift(w)"
            >
              从这天顺延
            </button></view
          >
        </view>
      </view>
      <view class="card">
        <view class="section-title">训练进步</view
        ><view class="muted"
          >每次正式组最大重量，统一换算 kg。热身组不计入正式组训练量。</view
        >
        <picker
          v-if="exerciseNames.length"
          :range="exerciseNames"
          :value="chartIndex"
          @change="chartIndex = Number($event.detail.value)"
          ><view class="pill">{{ exerciseNames[chartIndex] }} ▾</view></picker
        >
        <view v-if="!chartPoints.length" class="empty"
          >完成并保存训练后，这里会显示真实重量变化。</view
        >
        <view v-for="(point, pi) in chartPoints" :key="pi" class="chart-row"
          ><text>{{ point.date || point.performedDate }}</text
          ><view class="bar-track"
            ><view
              class="bar"
              :style="{
                width:
                  Math.max(
                    2,
                    (Number(point.maxWorkingWeightKg || 0) / chartMax) * 100,
                  ) + '%',
              }" /></view
          ><text>{{ point.maxWorkingWeightKg || 0 }} kg</text></view
        >
        <view
          v-for="(point, pi) in chartPoints"
          :key="'volume-' + pi"
          class="stat-line"
          >{{ point.date || point.performedDate }}：正式
          {{ point.workingSets }} 组 / 热身 {{ point.warmupSets }} 组 · 训练量
          {{ point.workingVolumeKg }} kg·次</view
        >
      </view>
    </view>
    <view v-if="logging" class="sheet-mask"
      ><scroll-view scroll-y class="sheet">
        <view class="section-title">{{ logging.title }} · 实际训练</view>
        <view class="row"
          ><text>实际日期</text
          ><picker
            mode="date"
            :value="performedDate"
            @change="performedDate = $event.detail.value"
            ><view class="pill">{{ performedDate }}</view></picker
          ></view
        >
        <view class="muted"
          >请填写本次实际完成的数据；目标重量仅供参考。RPE 为主观强度
          1–10。</view
        >
        <view v-for="ex in logging.exercises" :key="ex.key" class="exercise">
          <view class="name">{{ ex.name }}</view
          ><view class="muted">{{ ex.instructions }}</view>
          <image
            v-for="url in ex.imageUrls"
            :key="url"
            :src="url"
            mode="widthFix"
            class="guide-image"
            @tap="previewImage(url, ex.imageUrls)"
          />
          <video
            v-if="ex.videoUrl"
            :src="ex.videoUrl"
            controls
            class="guide-video"
          />
          <view class="set-head">组别 / 次数 / 实际重量 / 单位 / RPE</view>
          <view v-for="(set, si) in logSets" :key="si"
            ><view v-if="set.exerciseKey === ex.key" class="set-row">
              <picker
                :range="['热身', '正式']"
                :value="set.setType === 'warmup' ? 0 : 1"
                @change="
                  set.setType =
                    Number($event.detail.value) === 0 ? 'warmup' : 'working'
                "
                ><view class="pill">{{
                  set.setType === "warmup" ? "热身" : "正式"
                }}</view></picker
              >
              <input type="number" v-model.number="set.reps" /><input
                type="digit"
                v-model.number="set.weight"
                placeholder="实际重量"
              />
              <picker
                :range="['kg', 'lb']"
                :value="set.unit === 'kg' ? 0 : 1"
                @change="
                  set.unit = Number($event.detail.value) === 0 ? 'kg' : 'lb'
                "
                ><view class="pill">{{ set.unit }}</view></picker
              >
              <input
                type="digit"
                v-model.number="set.rpe"
                placeholder="RPE"
              /><text @tap="logSets.splice(si, 1)">×</text>
            </view></view
          >
          <button
            size="mini"
            @tap="
              logSets.push({
                ...emptySet(),
                exerciseKey: ex.key,
                exerciseName: ex.name,
                weight: '',
              })
            "
          >
            增加实际组
          </button>
        </view>
        <view class="actions"
          ><button :disabled="busy" @tap="logging = null">返回</button
          ><button
            class="primary"
            :disabled="busy"
            :loading="busy"
            @tap="saveLog"
          >
            保存实际训练
          </button></view
        >
      </scroll-view></view
    >
  </view>
</template>

<script>
import {
  trainingApi,
  uploadTrainingMedia,
  resolveTrainingMedia,
} from "../../api/training";
const clone = (value) => JSON.parse(JSON.stringify(value));
const today = () =>
  new Date(Date.now() + 8 * 3600000).toISOString().slice(0, 10);
export default {
  data() {
    return {
      role: "member",
      memberId: "",
      templates: [],
      plans: [],
      selected: null,
      editor: null,
      dayIndex: 0,
      workouts: [],
      points: [],
      chartIndex: 0,
      logging: null,
      logSets: [],
      startDate: today(),
      performedDate: today(),
      cycles: 4,
      loading: false,
      busy: false,
      error: "",
    };
  },
  computed: {
    currentDay() {
      return this.editor?.days[this.dayIndex];
    },
    exerciseNames() {
      return [...new Set(this.points.map((p) => p.exerciseName))];
    },
    chartPoints() {
      return this.points
        .filter((p) => p.exerciseName === this.exerciseNames[this.chartIndex])
        .slice(-30);
    },
    chartMax() {
      return Math.max(
        1,
        ...this.chartPoints.map((p) => Number(p.maxWorkingWeightKg || 0)),
      );
    },
  },
  onLoad(options) {
    this.role = options.role === "coach" ? "coach" : "member";
    this.memberId = options.memberId || "";
    this.load();
  },
  methods: {
    api(path, method = "GET", data) {
      return trainingApi(path, method, data, this.role);
    },
    async load() {
      this.loading = true;
      this.error = "";
      try {
        const [templates, plans] = await Promise.all([
          this.api("/templates"),
          this.api(
            "/plans",
            "GET",
            this.memberId ? { memberId: this.memberId } : {},
          ),
        ]);
        this.templates = templates || [];
        this.plans = plans || [];
      } catch (e) {
        this.error = e.message;
      } finally {
        this.loading = false;
      }
    },
    emptySet() {
      return { setType: "working", reps: 10, weight: 0, unit: "kg", rpe: 7 };
    },
    async act(fn) {
      if (this.busy) return;
      this.busy = true;
      try {
        await fn();
      } catch (e) {
        uni.showToast({ title: e.message || "操作失败", icon: "none" });
      } finally {
        this.busy = false;
      }
    },
    newPlan() {
      this.editor = {
        name: "",
        description: "",
        visibility: "private",
        cycleDays: 7,
        days: [],
      };
      this.dayIndex = 0;
      this.resizeDays();
    },
    async copyTemplate(t) {
      await this.act(async () => {
        const full = t.days ? t : await this.api(`/plans/${t.planId}`);
        this.editor = clone(full);
        delete this.editor.planId;
        delete this.editor.templateId;
        delete this.editor.status;
        this.editor.visibility = "private";
        this.editor.name += " · 我的版本";
        this.selected = null;
        this.dayIndex = 0;
      });
    },
    resizeDays() {
      const n = Number(this.editor.cycleDays);
      if (!Number.isInteger(n) || n < 1 || n > 31) {
        uni.showToast({ title: "循环为1–31天", icon: "none" });
        return;
      }
      const days = this.editor.days;
      this.editor.days = Array.from(
        { length: n },
        (_, i) =>
          days[i] || {
            dayNumber: i + 1,
            title: `第${i + 1}天`,
            restDay: true,
            exercises: [],
          },
      );
      this.dayIndex = Math.min(this.dayIndex, n - 1);
    },
    toggleRest(e) {
      this.currentDay.restDay = e.detail.value;
      if (e.detail.value) this.currentDay.exercises = [];
    },
    addExercise() {
      this.currentDay.exercises.push({
        key: `ex-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        name: "",
        instructions: "",
        imageUrls: [],
        videoUrl: "",
        targets: [this.emptySet()],
      });
    },
    savePlan() {
      this.act(async () => {
        this.resizeDays();
        const payload = clone(this.editor);
        if (this.memberId) payload.memberId = this.memberId;
        if (!payload.name.trim()) throw new Error("请填写计划名称");
        payload.days.forEach((d) =>
          d.exercises.forEach((ex) => {
            ex.imageUrls = ex.imageUrls.filter(Boolean);
            if (!ex.name.trim()) throw new Error("请填写动作名称");
          }),
        );
        await this.api(
          payload.planId ? `/plans/${payload.planId}` : "/plans",
          payload.planId ? "PUT" : "POST",
          payload,
        );
        this.editor = null;
        this.selected = null;
        await this.load();
        uni.showToast({ title: "计划已保存" });
      });
    },
    async openPlan(p) {
      await this.act(async () => {
        this.selected = await this.api(`/plans/${p.planId}`);
        await this.refreshPlan();
      });
    },
    async refreshPlan() {
      const [workouts, stats] = await Promise.all([
        this.api(`/plans/${this.selected.planId}/workouts`),
        this.api(`/plans/${this.selected.planId}/stats`),
      ]);
      this.workouts = workouts || [];
      this.points = stats?.points || [];
      this.chartIndex = 0;
    },
    editSelected() {
      this.editor = clone(this.selected);
      this.dayIndex = 0;
    },
    startPlan() {
      this.act(async () => {
        await this.api(`/plans/${this.selected.planId}/start`, "POST", {
          startDate: this.startDate,
          cycles: Number(this.cycles),
        });
        this.selected = await this.api(`/plans/${this.selected.planId}`);
        await this.refreshPlan();
      });
    },
    shift(w) {
      uni.showModal({
        title: "顺延未完成训练",
        content: `将 ${w.plannedDate} 起全部未完成训练顺延1天，已完成记录不动。约课时间不会随之调整。`,
        success: (res) => {
          if (res.confirm)
            this.act(async () => {
              await this.api(`/plans/${this.selected.planId}/shift`, "POST", {
                fromDate: w.plannedDate,
                days: 1,
              });
              await this.refreshPlan();
            });
        },
      });
    },
    async openLog(w) {
      await this.act(async () => {
        const workout = clone(w);
        for (const ex of workout.exercises || []) {
          ex.imageUrls = await Promise.all(
            (ex.imageUrls || []).map((url) =>
              resolveTrainingMedia(url, this.role),
            ),
          );
          if (ex.videoUrl)
            ex.videoUrl = await resolveTrainingMedia(ex.videoUrl, this.role);
        }
        this.logging = workout;
        this.performedDate = w.performedDate || today();
        this.logSets = w.sets?.length
          ? clone(w.sets)
          : (w.exercises || []).flatMap((ex) =>
              (ex.targets || []).map((s) => ({
                ...clone(s),
                weight: "",
                exerciseKey: ex.key,
                exerciseName: ex.name,
              })),
            );
      });
    },
    upload(ex, type) {
      const send = (filePath) =>
        this.act(async () => {
          const result = await uploadTrainingMedia(
            filePath,
            this.editor.planId,
            this.role,
          );
          if (type === "image") ex.imageUrls.push(result.url);
          else ex.videoUrl = result.url;
          uni.showToast({ title: "上传成功，请保存计划" });
        });
      if (type === "image")
        uni.chooseImage({ count: 1, success: (r) => send(r.tempFilePaths[0]) });
      else
        uni.chooseVideo({
          maxDuration: 60,
          compressed: true,
          success: (r) => {
            if (r.duration > 60) {
              uni.showToast({ title: "请选择60秒以内短视频", icon: "none" });
              return;
            }
            send(r.tempFilePath);
          },
        });
    },
    saveLog() {
      this.act(async () => {
        if (!this.logSets.length) throw new Error("请至少记录一组");
        if (this.logSets.some((s) => s.weight === "" || s.weight == null))
          throw new Error("请填写每组实际重量，自重动作填0");
        const sets = this.logSets.map((s) => ({
          ...s,
          reps: Number(s.reps),
          weight: Number(s.weight),
          rpe: s.rpe === "" || s.rpe == null ? null : Number(s.rpe),
        }));
        await this.api(`/workouts/${this.logging.workoutId}/log`, "PUT", {
          performedDate: this.performedDate,
          sets,
        });
        this.logging = null;
        await this.refreshPlan();
        uni.showToast({ title: "训练已记录" });
      });
    },
    previewImage(current, urls) {
      uni.previewImage({ current, urls });
    },
  },
};
</script>

<style scoped>
.training-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 28rpx 24rpx 80rpx;
  background: #f3f6f5;
  min-height: 100vh;
}
.hero {
  background: #163f36;
  color: #fff;
  padding: 40rpx;
  border-radius: 24rpx;
  margin-bottom: 24rpx;
}
.eyebrow {
  font-size: 20rpx;
  letter-spacing: 3rpx;
  color: #b8dbb4;
}
.heading {
  font-size: 42rpx;
  font-weight: 700;
  margin: 14rpx 0;
}
.hero .muted {
  color: #d0dfda;
}
.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 18rpx;
}
.muted {
  color: #6c7d77;
  font-size: 25rpx;
  line-height: 1.7;
}
.name {
  font-weight: 600;
  font-size: 29rpx;
}
.template-row,
.row {
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  align-items: center;
}
.template-row {
  padding: 24rpx 0;
  border-bottom: 1px solid #edf0ee;
}
.primary {
  background: #23614e !important;
  color: #fff !important;
}
.label {
  display: block;
  font-size: 26rpx;
  margin: 20rpx 0 8rpx;
}
input,
textarea {
  background: #f4f7f5;
  border: 1px solid #e0e9e3;
  border-radius: 10rpx;
  padding: 16rpx;
  margin: 8rpx 0;
  font-size: 27rpx;
  box-sizing: border-box;
}
input {
  height: 76rpx;
  flex: 1;
  min-width: 0;
}
textarea {
  width: 100%;
  height: 140rpx;
}
.small {
  flex: 0 0 110rpx !important;
  width: 110rpx;
}
.row {
  margin: 16rpx 0;
}
.day-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin: 24rpx 0;
}
.day-tab,
.pill {
  background: #edf3ef;
  border-radius: 10rpx;
  padding: 12rpx;
  font-size: 24rpx;
}
.day-tab.active {
  background: #23614e;
  color: #fff;
}
.exercise {
  background: #fafcfb;
  border: 1px solid #e4ece7;
  border-radius: 16rpx;
  padding: 18rpx;
  margin: 20rpx 0;
}
.danger,
.error {
  color: #ae3838;
}
.error {
  padding: 24rpx;
  background: #fff2f0;
}
.set-head {
  font-size: 22rpx;
  color: #6c7d77;
  margin-top: 20rpx;
}
.set-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}
.set-row input {
  width: 60rpx;
  padding: 10rpx;
  font-size: 24rpx;
}
.actions {
  display: flex;
  gap: 16rpx;
  margin-top: 22rpx;
}
.actions button {
  flex: 1;
  margin: 0;
  font-size: 26rpx;
}
.workout {
  padding: 20rpx 0;
  border-bottom: 1px solid #edf0ee;
}
.chart-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  font-size: 22rpx;
  margin: 18rpx 0;
}
.bar-track {
  flex: 1;
  background: #edf3ef;
  height: 24rpx;
  border-radius: 8rpx;
  overflow: hidden;
}
.bar {
  height: 100%;
  background: #3c9171;
}
.stat-line {
  font-size: 23rpx;
  line-height: 1.8;
  color: #64756c;
}
.empty {
  padding: 32rpx 0;
  color: #6c7d77;
}
.sheet-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 50;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.sheet {
  width: 100%;
  max-width: 900px;
  max-height: 90vh;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 30rpx;
  box-sizing: border-box;
}
.guide-image,
.guide-video {
  width: 100%;
  margin-top: 14rpx;
}
.guide-video {
  height: 360rpx;
}
button {
  margin-top: 16rpx;
  font-size: 26rpx;
}
</style>
