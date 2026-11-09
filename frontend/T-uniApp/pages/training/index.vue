<template>
  <view class="training-page">
    <view class="page-content">
      <view class="page-heading"
        ><view
          ><view class="page-title">{{
            editor ? "定制训练" : role === "coach" ? "学员训练" : "我的训练"
          }}</view></view
        ><button class="text-button" @tap="openLibrary(false)">
          动作库 ↗
        </button></view
      >
      <button
        v-if="role === 'coach' && !editor"
        class="back-link"
        @tap="backToStudents"
      >
        ‹ 返回学员列表
      </button>
      <view v-if="role === 'coach' && memberId" class="member-context card">
        <text class="eyebrow accent">当前学员</text>
        <view class="name">{{ memberContext ? memberContext.displayName : '正在确认学员…' }}</view>
        <view class="hint">仅绑定教练可编辑。</view>
      </view>
      <view v-if="!authenticated" class="card empty-card"
        ><view class="section-title">我的训练</view
        ><button class="primary space-top" @tap="goLogin">会员登录</button
        ><button class="secondary space-top" @tap="openLibrary(false)">动作库</button></view
      >
      <view v-else-if="role === 'coach' && !memberId" class="card empty-card"
        ><view class="section-title">先选一位学员</view
        ><view class="muted space-top"
          >从学员列表进入，为已绑定的学员制定训练计划。</view
        ><button class="primary space-top" @tap="backToStudents">
          选择学员
        </button></view
      >
      <view v-else>
        <view v-if="error" class="error-box" @tap="load"
          >{{ error }}<view class="accent space-top">点击重试 ↻</view></view
        >
        <view v-if="loading && !editor" class="loading-state"
          >正在整理训练安排…</view
        >
        <view v-if="!selected && !editor && !loading"><view class="card focus-card"
            ><view class="section-head"
              ><text class="eyebrow accent">{{ focusLabel }}</text
              ><text v-if="focusWorkout" class="muted">{{
                shortDate(focusWorkout.plannedDate)
              }}</text></view
            ><view v-if="focusWorkout"
              ><view class="focus-title">{{ focusWorkout.title }}</view
              ><view class="muted"
                >{{ featuredPlan.name }} ·
                {{
                  focusWorkout.status === "rest"
                    ? "休息日"
                    : exerciseSummary(focusWorkout)
                }}</view
              ><button
                class="primary space-top"
                :disabled="busy"
                @tap="openFocus"
              >
                {{
                  focusWorkout.status === "rest" ||
                  focusWorkout.plannedDate > currentDate
                    ? "查看训练日程"
                    : "进入训练"
                }}
                →
              </button></view
            ><view v-else
              ><view class="focus-title">{{
                featuredPlan ? "安排下一次训练" : "暂无训练计划"
              }}</view
              ><view class="muted">{{
                featuredPlan
                  ? "可查看日程或续排循环。"
                  : "可新建或从下方模板开始。"
              }}</view
              ><button
                class="primary space-top"
                :disabled="busy"
                @tap="featuredPlan ? openPlan(featuredPlan) : newPlan()"
              >
                {{ featuredPlan ? "打开训练计划" : "制定我的计划" }} →
              </button></view
            ></view
          >
          <view class="section-head section-space"
            ><text class="section-title">{{
              role === "coach" ? "学员的计划" : "我的计划"
            }}</text
            ><button class="text-button accent" @tap="newPlan">
              ＋ 新建
            </button></view
          >
          <view v-if="!plans.length" class="card empty-card"
            ><view class="name">还没有训练计划</view
            ></view
          >
          <view
            v-for="plan in plans"
            :key="plan.planId"
            class="card plan-card"
            @tap="openPlan(plan)"
            ><view class="plan-icon">{{ plan.cycleDays }}<text>天</text></view
            ><view class="flex-one"
              ><view class="name">{{ plan.name }}</view
              ><view class="muted"
                >{{ plan.cycleDays }} 天一循环 ·
                {{
                  plan.visibility === "public" ? "公开计划" : "私人计划"
                }}</view
              ></view
            ><view class="plan-end"
              ><text
                :class="['badge', { 'badge-accent': plan.status === 'active' }]"
                >{{ planStatus(plan) }}</text
              ><text class="arrow">↗</text></view
            ></view
          >
          <view class="section-head section-space"
            ><text class="section-title">免费计划库</text
            ></view
          >
          <view
            v-for="item in templates"
            :key="item.templateId || item.planId"
            class="card template-card"
            ><view class="template-title">{{ item.name }}</view
            ><view class="muted"
              >{{ item.cycleDays }} 天循环<text v-if="trainingDays(item)">
                · {{ trainingDays(item) }} 个训练日</text
              ></view
            ><view v-if="item.description" class="template-description">{{
              item.description
            }}</view
            ><button
              class="secondary space-top"
              :disabled="busy"
              @tap="copyTemplate(item)"
            >
              按此定制 →
            </button></view
          >
          <view v-if="!templates.length && !error" class="card empty-card"
            ><view class="muted">暂时没有可用模板，可以先创建自己的计划。</view
            ><button class="secondary space-top" @tap="newPlan">
              新建计划
            </button></view
          >
          <view class="library-banner" @tap="openLibrary(false)"
            ><view
              ><view class="name">动作指南</view
              ><view class="muted">按肌群或器械筛选</view></view
            ><text class="accent library-arrow">↗</text></view
          >
        </view>
        <view v-if="selected && !editor && !loading">
          <button class="back-link" @tap="backToPlans">‹ 所有训练计划</button>
          <view class="card"
            ><view class="section-head"
              ><text class="badge badge-accent">{{
                planStatus(selected)
              }}</text
              ><button v-if="canEditPlan" class="text-button" :disabled="busy" @tap="editSelected">
                编辑计划 ↗
              </button></view
            ><view class="detail-title">{{ selected.name }}</view
            ><view v-if="selected.description" class="muted">{{
              selected.description
            }}</view
            ><view class="metrics"
              ><view
                ><text class="metric-value"
                  >{{ selected.cycleDays
                  }}<text class="metric-unit">天</text></text
                ><text class="metric-label">循环周期</text></view
              ><view
                ><text class="metric-value"
                  >{{ completedWorkouts
                  }}<text class="metric-unit">次</text></text
                ><text class="metric-label">已完成训练</text></view
              ><view
                ><text class="metric-value"
                  >{{ trainingDays(selected)
                  }}<text class="metric-unit">天</text></text
                ><text class="metric-label">每循环训练日</text></view
              ></view
            >
            <view v-if="selected.status === 'draft'" class="start-form"
              ><view class="form-row"
                ><text>开始日期</text
                ><picker
                  mode="date"
                  :value="startDate"
                  @change="startDate = $event.detail.value"
                  ><view class="picker-box">{{ startDate }} ▾</view></picker
                ></view
              ><view class="form-row"
                ><view
                  ><text>循环次数</text
                  ><view class="hint"
                    >共 {{ startTotalDays }} 天，日期按北京时间</view
                  ></view
                ><input
                  class="number-input"
                  type="number"
                  v-model.number="cycles" /></view
              ><button
                class="primary space-top"
                :disabled="busy"
                :loading="busy"
                @tap="startPlan"
              >
                开启训练计划
              </button></view
            >
            <view class="actions lifecycle-actions">
              <button class="secondary compact" :disabled="busy" @tap="copySelected">复制为私人新计划</button>
              <button v-if="selected.status === 'active' || selected.status === 'completed'" class="primary compact" :disabled="busy" @tap="showExtend">续排循环</button>
            </view>
            <view v-if="extending" class="start-form">
              <view class="form-row"><text>新一轮开始日期</text><picker mode="date" :value="extendDate" :start="earliestExtendDate" :end="addDays(currentDate, 366)" @change="extendDate = $event.detail.value"><view class="picker-box">{{ extendDate }} ▾</view></picker></view>
              <view class="form-row"><view><text>追加循环次数</text><view class="hint">追加 {{ Number(extendCycles || 0) * selected.cycleDays }} 天，原日程和记录保留。</view></view><input class="number-input" type="number" v-model.number="extendCycles" /></view>
              <button class="primary" :disabled="busy" @tap="extendPlan">确认续排</button>
            </view>
            <view class="actions">
              <button v-if="selected.status === 'active' && !pendingWorkoutCount" class="text-button" :disabled="busy" @tap="completePlan">标记计划完成</button>
              <button v-if="selected.status !== 'archived'" class="text-button" :disabled="busy" @tap="archivePlan">归档并停止后续安排</button>
            </view>
            <view v-if="selected.status === 'completed'" class="hint">本轮训练已全部完成。可续排下一轮，也可复制后调整循环长度。</view>
            <view v-if="selected.status === 'archived'" class="hint">已停止后续安排，历史训练只读保留。复制后可重新编辑和开启。</view>
          </view>
          <view class="segmented"
            ><view
              v-for="tab in detailTabs"
              :key="tab.key"
              :class="['segment', { active: detailTab === tab.key }]"
              @tap="detailTab = tab.key"
              >{{ tab.label }}</view
            ></view
          >
          <view v-if="detailTab === 'schedule'">
            <view v-if="!workouts.length" class="card empty-card"
              ><view class="name">开启后，训练会排进日历</view
              ><view class="muted"
                >上方选择开始日期和循环次数，就能生成每天的安排。</view
              ></view
            >
            <view v-else
              ><view class="filter-row"
                ><view
                  v-for="filter in scheduleFilters"
                  :key="filter.key"
                  :class="[
                    'filter-chip',
                    { active: scheduleFilter === filter.key },
                  ]"
                  @tap="setScheduleFilter(filter.key)"
                  >{{ filter.label }}</view
                ></view
              ><view class="hint schedule-tip"
                >有事没练？选择新的日期，预览这天及后续尚未开始的安排。</view
              ><view v-if="!filteredWorkouts.length" class="card empty-card"
                ><view class="name">{{
                  scheduleFilter === "completed"
                    ? "还没有完成记录"
                    : "这里暂时没有训练安排"
                }}</view
                ><view class="muted">{{
                  scheduleFilter === "completed"
                    ? "完成第一组，记录你的起点。"
                    : "切换到全部，查看完整日程。"
                }}</view
                ><button
                  class="secondary space-top"
                  @tap="setScheduleFilter('all')"
                >
                  查看全部日程
                </button></view
              >
              <view
                v-for="workout in visibleWorkouts"
                :key="workout.workoutId"
                :class="[
                  'card workout-card',
                  {
                    'workout-today': workout.plannedDate === currentDate,
                    'workout-rest': workout.status === 'rest',
                  },
                ]"
                ><view class="workout-date"
                  ><text class="date-day">{{
                    workout.plannedDate.slice(8)
                  }}</text
                  ><text class="date-month"
                    >{{ workout.plannedDate.slice(5, 7) }} 月</text
                  ></view
                ><view class="flex-one"
                  ><view class="section-head"
                    ><view class="name">{{ workout.title }}</view
                    ><text
                      :class="[
                        'badge',
                        { 'badge-accent': workout.plannedDate === currentDate },
                      ]"
                      >{{
                        workout.plannedDate === currentDate
                          ? "今天"
                          : statusLabel(workout)
                      }}</text
                    ></view
                  ><view class="muted">{{
                    workout.status === "rest"
                      ? "恢复也是训练的一部分"
                      : exerciseSummary(workout)
                  }}</view
                  ><view
                    v-if="workout.originalDate !== workout.plannedDate"
                    class="hint"
                    >原定 {{ workout.originalDate }} · 已顺延</view
                  ><view v-if="workout.status === 'completed'" class="hint"
                    >实际完成于 {{ workout.performedDate }}</view
                  ><view v-if="workout.status !== 'rest' && workout.status !== 'cancelled'" class="actions"
                    ><button
                      :class="
                        workout.status === 'completed'
                          ? 'secondary compact'
                          : 'primary compact'
                      "
                      :disabled="busy"
                      @tap="openLog(workout)"
                    >
                      {{
                        workout.status === "completed" ? "查看记录" : workout.status === "in_progress" ? "继续训练" : "记录训练"
                      }}</button
                    ><button
                      v-if="workout.status === 'planned' && selected.status === 'active'"
                      class="secondary compact"
                      :disabled="busy"
                      @tap="shift(workout)"
                    >
                      调整日期
                    </button></view
                  ><button
                    v-else-if="workout.status === 'rest' && selected.status === 'active'"
                    class="text-button rest-shift"
                    :disabled="busy"
                    @tap="shift(workout)"
                  >
                    从这天调整日期 →
                  </button></view
                ></view
              >
              <button
                v-if="filteredWorkouts.length > scheduleLimit"
                class="secondary"
                @tap="scheduleLimit += 20"
              >
                再看 20 天
              </button>
            </view>
          </view>
          <view v-if="detailTab === 'progress'" class="card"
            ><view class="section-title">训练重量</view
            ><view class="muted space-top"
              >按每次正式组的最大重量查看。统一换算为 kg，热身组单独计数。</view
            ><picker
              v-if="chartExercises.length"
              :range="chartExercises"
              range-key="name"
              :value="chartIndex"
              @change="chartIndex = Number($event.detail.value)"
              ><view class="picker-box chart-picker"
                >{{ chartExercises[chartIndex].name }} <text>⌄</text></view
              ></picker
            ><view v-if="!chartPoints.length" class="empty-card"
              ><view class="name">暂无训练记录</view
              ><view class="muted">保存实际训练后，这里会出现重量变化。</view
              ><button
                class="secondary space-top"
                @tap="detailTab = 'schedule'"
              >
                去记录训练
              </button></view
            ><view
              v-for="(point, index) in chartPoints"
              :key="point.workoutId + '-' + index"
              class="chart-entry"
              ><view class="section-head"
                ><text class="muted">{{ point.date }}</text
                ><text class="chart-weight"
                  ><template v-if="point.maxWorkingWeightKg != null"
                    >{{ formatWeight(point.maxWorkingWeightKg)
                    }}<text class="metric-unit">kg</text></template
                  ><template v-else>暂无正式组</template></text
                ></view
              ><view class="bar-track"
                ><view
                  class="bar"
                  :style="{ width: barWidth(point) + '%' }" /></view
              ><view class="chart-caption"
                >正式 {{ point.workingSets }} 组 · 热身
                {{ point.warmupSets }} 组<text
                  >训练量 {{ formatWeight(point.workingVolumeKg) }} kg·次</text
                ></view
              ></view
            ></view
          >
          <view v-if="detailTab === 'outline'"
            ><view class="hint schedule-tip"
              >编辑只更新尚未开始的安排，已暂存和已完成的训练保持原有动作与目标。</view
            ><view
              v-for="day in selected.days"
              :key="day.dayNumber"
              class="card"
              ><view class="section-head"
                ><text class="eyebrow accent">第 {{ day.dayNumber }} 天</text
                ><text class="badge">{{
                  day.restDay ? "休息日" : day.exercises.length + " 个动作"
                }}</text></view
              ><view class="name space-top">{{ day.title }}</view
              ><view v-if="day.restDay" class="muted space-top"
                >休息日</view
              ><view
                v-for="exercise in day.exercises"
                :key="exercise.key"
                class="outline-exercise"
                ><view class="name">{{ exercise.name }}</view
                ><view class="muted">{{ targetSummary(exercise) }}</view
                ><view v-if="exercise.instructions" class="guide-copy">{{
                  exercise.instructions
                }}</view></view
              ></view
            ><button v-if="canEditPlan" class="primary" :disabled="busy" @tap="editSelected">
              调整动作与目标
            </button></view
          >
        </view>
        <view v-if="editor" class="editor-content">

          <view class="card"
            ><text class="field-label">计划名称</text
            ><input
              v-model="editor.name"
              maxlength="100"
              placeholder="例如：我的三分化训练"
              placeholder-class="input-placeholder" /><text class="field-label"
              >训练目标与备注</text
            ><textarea
              v-model="editor.description"
              maxlength="2000"
              placeholder="想练什么、每周练几天，记在这里"
              placeholder-class="input-placeholder" /><view class="form-row"
              ><view
                ><text>循环天数</text
                ><view class="hint">{{
                  editor.status === "active"
                    ? "已开启的计划保留原有循环长度"
                    : "包含训练日与休息日，最多 31 天"
                }}</view></view
              ><input
                class="number-input"
                type="number"
                :disabled="editor.status === 'active'"
                v-model.number="editor.cycleDays"
                @blur="changeCycleDays" /></view
            ><view class="form-row"
              ><view
                ><text>公开免费分享</text
                ><view class="hint"
                  >分享动作和目标，实际重量记录仍然私密。</view
                ></view
              ><switch
                color="#ff7a35"
                :checked="editor.visibility === 'public'"
                @change="
                  editor.visibility = $event.detail.value ? 'public' : 'private'
                " /></view
          ></view>
          <view v-if="cycleBackup" class="card cycle-warning">
            <view class="hint">刚移出循环末尾的日期。保存前仍可恢复。</view>
            <button class="secondary compact space-top" :disabled="busy" @tap="restoreCycleDays">恢复为 {{ cycleBackup.length }} 天循环</button>
          </view>
          <scroll-view
            scroll-x
            class="horizontal-scroll"
            :scroll-into-view="'day-tab-' + dayIndex"
            scroll-with-animation
            ><view class="day-tabs"
              ><view
                v-for="(day, index) in editor.days"
                :id="'day-tab-' + index"
                :key="day.dayNumber"
                :class="['day-tab', { active: dayIndex === index }]"
                @tap="dayIndex = index"
                ><text>第 {{ day.dayNumber }} 天</text
                ><text class="day-caption">{{
                  day.restDay ? "休息" : day.title
                }}</text></view
              ></view
            ></scroll-view
          >
          <view v-if="currentDay"
            ><view class="card"
              ><text class="field-label">当天主题</text
              ><input
                v-model="currentDay.title"
                maxlength="100"
                placeholder="例如：推 · 胸肩三头"
                placeholder-class="input-placeholder" /><view class="form-row"
                ><view
                  ><text>安排为休息日</text
                  ></view
                ><switch
                  color="#ff7a35"
                  :checked="currentDay.restDay"
                  @change="toggleRest" /></view></view
            ><view v-if="currentDay.restDay" class="card rest-card"
              ><view class="rest-symbol">○</view
              ><view class="name">休息日</view
              ></view
            >
            <view v-else
              ><view v-if="!currentDay.exercises.length" class="card empty-card"
                ><view class="name">训练动作</view
                ><view class="muted"
                  >从动作库选一个，或添加自己的动作。</view
                ></view
              >
              <view
                v-for="(exercise, exerciseIndex) in currentDay.exercises"
                :key="exercise.key"
                class="card edit-exercise"
                ><view class="section-head"
                  ><text class="eyebrow accent"
                    >动作 {{ exerciseIndex + 1 }}</text
                  ><button
                    class="text-button danger"
                    @tap="removeExercise(exerciseIndex)"
                  >
                    移除
                  </button></view
                ><input
                  v-model="exercise.name"
                  maxlength="100"
                  placeholder="动作名称"
                  placeholder-class="input-placeholder"
                /><textarea
                  v-model="exercise.instructions"
                  maxlength="4000"
                  placeholder="动作要领、节奏或持续时间"
                  placeholder-class="input-placeholder"
                /><view class="section-head set-section-heading"
                  ><view
                    ><view class="name">目标组</view
                    ><view class="hint">重量可留空；自重填 0。</view></view
                  ><text class="hint">kg 公斤 / lb 磅</text></view
                >
                <view
                  v-for="(set, setIndex) in exercise.targets"
                  :key="setIndex"
                  :class="[
                    'target-set',
                    { 'warmup-set': set.setType === 'warmup' },
                  ]"
                  ><view class="section-head"
                    ><picker
                      :range="setTypes"
                      :value="set.setType === 'warmup' ? 0 : 1"
                      @change="
                        set.setType =
                          Number($event.detail.value) === 0
                            ? 'warmup'
                            : 'working'
                      "
                      ><view
                        :class="[
                          'set-type',
                          { warmup: set.setType === 'warmup' },
                        ]"
                        >{{ set.setType === "warmup" ? "热身" : "正式" }} · 第
                        {{ setIndex + 1 }} 组 ⌄</view
                      ></picker
                    ><button
                      class="remove-button"
                      @tap="removeTarget(exercise, setIndex)"
                    >
                      ×
                    </button></view
                  ><view class="set-inputs"
                    ><view class="set-field"
                      ><text class="input-caption">次数</text
                      ><input
                        type="number"
                        v-model.number="set.reps"
                        placeholder="10"
                        placeholder-class="input-placeholder" /></view
                    ><view class="set-field"
                      ><text class="input-caption">目标重量</text
                      ><input
                        type="digit"
                        v-model.number="set.weight"
                        placeholder="可留空"
                        placeholder-class="input-placeholder" /></view
                    ><view class="unit-field"
                      ><text class="input-caption">单位</text
                      ><picker
                        :range="units"
                        :value="set.unit === 'lb' ? 1 : 0"
                        @change="
                          set.unit =
                            Number($event.detail.value) === 0 ? 'kg' : 'lb'
                        "
                        ><view class="unit-picker"
                          >{{ set.unit }} ⌄</view
                        ></picker
                      ></view
                    ><view class="set-field rpe-field"
                      ><text class="input-caption">强度</text
                      ><input
                        type="digit"
                        v-model.number="set.rpe"
                        placeholder="选填"
                        placeholder-class="input-placeholder" /></view></view
                ></view>
                <view class="actions"
                  ><button
                    class="secondary compact"
                    @tap="addTarget(exercise, 'warmup')"
                  >
                    ＋ 热身组</button
                  ><button
                    class="secondary compact"
                    @tap="addTarget(exercise, 'working')"
                  >
                    ＋ 正式组
                  </button></view
                ><view class="hint space-top"
                  >强度 RPE：1–10，数值越高越接近力竭。</view
                >
                <view class="media-section"
                  ><view class="name">动作素材</view
                  ><view v-if="exercise.imageUrls.length" class="media-grid"
                    ><view
                      v-for="(url, imageIndex) in exercise.imageUrls"
                      :key="url"
                      class="media-tile"
                      ><image
                        v-if="mediaViews[url]"
                        :src="mediaViews[url]"
                        mode="aspectFill"
                        @error="mediaPreviewFailed(url)"
                        @tap="previewMedia(url, exercise.imageUrls)"
                      /><view
                        v-else
                        class="media-placeholder"
                        @tap="retryMedia(url)"
                        >{{ mediaFailed[url] ? "点击重试" : "读取图片…" }}</view
                      ><button
                        class="media-remove"
                        @tap="exercise.imageUrls.splice(imageIndex, 1)"
                      >
                        ×
                      </button></view
                    ></view
                  ><view v-if="exercise.videoUrl"
                    ><video
                      v-if="mediaViews[exercise.videoUrl]"
                      :src="mediaViews[exercise.videoUrl]"
                      @error="mediaPreviewFailed(exercise.videoUrl)"
                      controls
                      class="guide-video"
                    /><view
                      v-else
                      class="media-placeholder"
                      @tap="retryMedia(exercise.videoUrl)"
                      >{{
                        mediaFailed[exercise.videoUrl]
                          ? "视频未读取，点击重试"
                          : "点此加载视频"
                      }}</view
                    ><button
                      class="text-button danger"
                      @tap="exercise.videoUrl = ''"
                    >
                      移除视频
                    </button></view
                  ><view class="actions"
                    ><button
                      class="secondary compact"
                      :disabled="busy"
                      @tap="upload(exercise, 'image')"
                    >
                      ＋ 图片</button
                    ><button
                      class="secondary compact"
                      :disabled="busy"
                      @tap="upload(exercise, 'video')"
                    >
                      ＋ 视频
                    </button></view
                  ><view class="hint space-top">每张图片最多 10 MB，每个 MP4 视频最多 50 MB。</view
                  ><view v-if="!editor.planId" class="hint">首次上传时会保存当前计划。</view></view
                >
              </view>
              <button class="primary" @tap="openLibrary(true)">
                ＋ 从动作库添加</button
              ><button class="secondary space-top" @tap="addExercise()">
                添加自定义动作
              </button>
            </view>
          </view>
          <view class="editor-bottom"
            ><button class="secondary" :disabled="busy" @tap="cancelEditor">
              取消</button
            ><button
              class="primary"
              :disabled="busy"
              :loading="busy"
              @tap="savePlan"
            >
              保存计划
            </button></view
          >
        </view>
      </view>
    </view>
    <T1Nav
      v-if="role === 'member' && !editor && !logging && !libraryOpen && !shifting"
      role="member"
      current="training"
    />
    <view v-if="shifting" class="sheet-mask shift-mask">
      <view class="sheet shift-sheet">
        <view class="sheet-header"><view><text class="eyebrow accent">调整训练日期</text><view class="section-title">{{ shifting.title }}</view></view><button class="close-button" :disabled="busy" @tap="shifting = null">×</button></view>
        <view class="sheet-body shift-controls">
          <view class="form-row"><text>原定 {{ shifting.plannedDate }}</text><picker mode="date" :value="shiftDate" :start="addDays(shifting.plannedDate, 1)" :end="addDays(shifting.plannedDate, 365)" @change="shiftDate = $event.detail.value; shiftPreview = null"><view class="picker-box">移到 {{ shiftDate }} ▾</view></picker></view>
          <button class="secondary" :disabled="busy" :loading="busy" @tap="previewShift">预览受影响日程</button>
          <view v-if="shiftPreview" class="hint space-top">{{ shiftPreview.message }} · 共 {{ shiftPreview.affectedCount }} 天</view>
        </view>
        <scroll-view scroll-y class="library-scroll"><view class="sheet-body">
          <view v-for="change in (shiftPreview ? shiftPreview.changes : [])" :key="change.workoutId" class="shift-change"><view class="name">{{ change.title }}</view><view class="muted">{{ change.beforeDate }} → {{ change.afterDate }}</view></view>
        </view></scroll-view>
        <view class="log-footer"><button class="primary" :disabled="busy || !shiftPreview || !shiftPreview.canApply" @tap="applyShift">按预览确认顺延</button></view>
      </view>
    </view>
    <view v-if="libraryOpen" class="sheet-mask"
      ><view class="sheet library-sheet"
        ><view class="sheet-header"
          ><view
            ><view class="section-title">{{
              libraryPicking ? "为今天添加动作" : "找到你的训练动作"
            }}</view></view
          ><button class="close-button" @tap="libraryOpen = false">
            ×
          </button></view
        ><view class="library-search"
          ><input
            v-model="libraryQuery"
            placeholder="搜索动作、肌群或器械"
            placeholder-class="input-placeholder"
          /><view class="filter-pickers"
            ><picker
              :range="muscleFilters"
              :value="muscleIndex"
              @change="muscleIndex = Number($event.detail.value)"
              ><view class="picker-box"
                >{{
                  muscleIndex ? muscleFilters[muscleIndex] : "所有肌群"
                }}
                ⌄</view
              ></picker
            ><picker
              :range="equipmentFilters"
              :value="equipmentIndex"
              @change="equipmentIndex = Number($event.detail.value)"
              ><view class="picker-box"
                >{{
                  equipmentIndex ? equipmentFilters[equipmentIndex] : "所有器械"
                }}
                ⌄</view
              ></picker
            ></view
          ><view class="hint"
            >{{ libraryResults.length }} 个动作 · 选择能够稳定控制的重量</view
          ></view
        ><scroll-view scroll-y class="library-scroll"
          ><view class="sheet-body"
            ><view v-if="!libraryResults.length" class="empty-card"
              ><view class="name">没有找到这个动作</view
              ><view class="muted">换一个关键词，或放宽肌群和器械筛选。</view
              ><button class="secondary space-top" @tap="resetLibrary">
                清除筛选
              </button></view
            ><view
              v-for="exercise in libraryResults"
              :key="exercise.key"
              class="library-exercise"
              ><view class="section-head"
                ><view
                  ><view class="name">{{ exercise.name }}</view
                  ><view class="tag-row"
                    ><text class="badge">{{ exercise.muscle }}</text
                    ><text class="badge">{{ exercise.equipment }}</text></view
                  ></view
                ><button
                  v-if="libraryPicking"
                  class="add-library-button"
                  @tap="addExercise(exercise)"
                >
                  ＋
                </button></view
              ><view class="muted space-top">{{ exercise.summary }}</view
              ><view
                v-for="(step, index) in exercise.steps"
                :key="index"
                class="guide-step"
                ><text class="step-number">{{ index + 1 }}</text
                ><text>{{ step }}</text></view
              ><view class="exercise-cues">{{
                exercise.cues.join(" · ")
              }}</view></view
            ><button
              v-if="libraryPicking"
              class="secondary"
              @tap="addExercise()"
            >
              没有合适的？添加自定义动作
            </button></view
          ></scroll-view
        ></view
      ></view
    >
    <view v-if="logging" class="sheet-mask log-mask"
      ><view class="sheet log-sheet"
        ><view class="sheet-header"
          ><view
            ><text class="eyebrow accent">{{
              logging.status === "completed" ? "训练记录" : "本次训练"
            }}</text
            ><view class="section-title">{{ logging.title }}</view></view
          ><button class="close-button" :disabled="busy" @tap="closeLog">
            ×
          </button></view
        >
        <view v-if="!logReadOnly" class="rest-timer"
          ><view class="timer-main"
            ><view
              ><text class="timer-label">组间休息</text
              ><view class="timer-time">{{ restDisplay }}</view></view
            ><view class="timer-controls"
              ><view class="timer-presets"
                ><view
                  v-for="seconds in restPresets"
                  :key="seconds"
                  :class="[
                    'timer-preset',
                    { active: restDuration === seconds },
                  ]"
                  @tap="setRestDuration(seconds)"
                  >{{ seconds }} 秒</view
                ></view
              ><view class="timer-buttons"
                ><button class="text-button accent" @tap="toggleRestTimer">
                  {{
                    restRunning
                      ? "暂停"
                      : restRemaining > 0 && restRemaining < restDuration
                        ? "继续"
                        : "开始"
                  }}</button
                ><button class="text-button" @tap="resetRestTimer">
                  重置
                </button></view
              ></view
            ></view
          ><view class="timer-hint">{{
            restFinished
              ? "休息结束，准备好再开始下一组。"
              : "每完成一组会开始计时；切回页面按实际时间继续。"
          }}</view></view
        >
        <scroll-view scroll-y class="log-scroll"
          ><view class="sheet-body log-body"
            ><view class="form-row"
              ><view
                ><text>实际训练日期</text
                ><view class="hint">按北京时间记录</view></view
              ><picker
                mode="date" :disabled="logReadOnly || busy"
                :value="performedDate"
                :start="selected && selected.startDate"
                :end="currentDate"
                @change="performedDate = $event.detail.value"
                ><view class="picker-box">{{ performedDate }} ⌄</view></picker
              ></view
            ><view class="log-progress"
              ><view class="section-head"
                ><text>本次完成</text
                ><text class="accent"
                  >{{ completedLogSets }} / {{ logSets.length }} 组</text
                ></view
              ><view class="bar-track"
                ><view
                  class="bar"
                  :style="{ width: logProgress + '%' }" /></view
              ><view class="hint"
                >填入实际重量，点勾完成一组；自重动作填 0。</view
              ></view
            >
            <view
              v-for="(exercise, exerciseIndex) in logging.exercises"
              :key="exercise.key"
              class="log-exercise"
              ><view class="section-head"
                ><text class="eyebrow accent">{{ pad(exerciseIndex + 1) }}</text
                ><text class="badge">{{ targetSummary(exercise) }}</text></view
              ><view class="log-exercise-name">{{ exercise.name }}</view
              ><view v-if="previousFor(exercise).length" class="previous-result"
                ><view
                  ><text class="hint"
                    >上次正式组 · {{ previousDate(exercise) }}</text
                  ><view class="previous-weight">{{
                    previousSummary(exercise)
                  }}</view></view
                ><button
                  class="text-button accent"
                  v-if="!logReadOnly" :disabled="busy" @tap="usePrevious(exercise)"
                >
                  沿用上次
                </button></view
              ><view v-else class="hint"
                >暂无历史记录</view
              ><view
                v-if="
                  exercise.instructions ||
                  exercise.imageUrls.length ||
                  exercise.videoUrl
                "
                class="instruction-toggle"
                @tap="toggleGuide(exercise.key)"
                ><text>动作要领与素材</text
                ><text>{{
                  expandedGuides[exercise.key] ? "收起 −" : "展开 ＋"
                }}</text></view
              ><view v-if="expandedGuides[exercise.key]"
                ><view class="guide-copy">{{ exercise.instructions }}</view
                ><view class="media-grid"
                  ><view
                    v-for="url in exercise.imageUrls"
                    :key="url"
                    class="media-tile"
                    ><image
                      v-if="mediaViews[url]"
                      :src="mediaViews[url]"
                      mode="aspectFill"
                      @error="mediaPreviewFailed(url)"
                      @tap="previewMedia(url, exercise.imageUrls)"
                    /><view
                      v-else
                      class="media-placeholder"
                      @tap="retryMedia(url)"
                      >{{ mediaFailed[url] ? "点击重试" : "读取图片…" }}</view
                    ></view
                  ></view
                ><video
                  v-if="exercise.videoUrl && mediaViews[exercise.videoUrl]"
                  :src="mediaViews[exercise.videoUrl]"
                  @error="mediaPreviewFailed(exercise.videoUrl)"
                  controls
                  class="guide-video"
                /><button
                  v-if="exercise.videoUrl && !mediaViews[exercise.videoUrl]"
                  class="text-button"
                  @tap="retryMedia(exercise.videoUrl)"
                >
                  {{
                    mediaFailed[exercise.videoUrl]
                      ? "重试读取视频"
                      : "查看动作视频"
                  }}
                </button></view
              >
              <view
                v-for="group in setGroups"
                :key="group.key"
                class="log-set-group"
                ><view class="group-heading"
                  ><text
                    :class="group.key === 'warmup' ? 'warmup-label' : 'accent'"
                    >{{ group.label }}</text
                  ><text class="hint">{{
                    group.key === "warmup"
                      ? "渐进热身，找到状态"
                      : "只记录实际完成的重量"
                  }}</text></view
                ><view v-for="(set, setIndex) in logSets" :key="set.localId"
                  ><view
                    v-if="
                      set.exerciseKey === exercise.key &&
                      set.setType === group.key
                    "
                    :class="['actual-set', { 'set-complete': set.done }]"
                    ><view class="actual-set-head"
                      ><text class="hint">第 {{ setNumber(setIndex) }} 组</text
                      ><button
                        class="remove-button"
                        v-if="!logReadOnly" :disabled="busy" @tap="removeLogSet(setIndex)"
                      >
                        ×
                      </button></view
                    ><view class="set-inputs"
                      ><view class="set-field"
                        ><text class="input-caption">实际次数</text
                        ><input
                          type="number"
                          v-model.number="set.reps" :disabled="logReadOnly || busy"
                          placeholder="次数"
                          placeholder-class="input-placeholder" /></view
                      ><view class="set-field"
                        ><text class="input-caption">实际重量</text
                        ><input
                          type="digit"
                          v-model.number="set.weight" :disabled="logReadOnly || busy"
                          placeholder="必填"
                          placeholder-class="input-placeholder" /></view
                      ><view class="unit-field"
                        ><text class="input-caption">单位</text
                        ><picker
                          :range="units" :disabled="logReadOnly || busy"
                          :value="set.unit === 'lb' ? 1 : 0"
                          @change="
                            set.unit =
                              Number($event.detail.value) === 0 ? 'kg' : 'lb'
                          "
                          ><view class="unit-picker"
                            >{{ set.unit }} ⌄</view
                          ></picker
                        ></view
                      ><button
                        :class="['complete-set-button', { checked: set.done }]"
                        :disabled="logReadOnly || busy" @tap="completeSet(set)"
                      >
                        {{ set.done ? "✓" : "○" }}
                      </button></view
                    ><view class="set-extra"
                      ><text class="hint">主观强度 RPE</text
                      ><input
                        type="digit"
                        v-model.number="set.rpe" :disabled="logReadOnly || busy"
                        placeholder="选填 1–10"
                        placeholder-class="input-placeholder"
                      /><text
                        v-if="set.targetWeight != null"
                        class="hint target-reference"
                        >目标 {{ set.targetWeight }} {{ set.targetUnit }}</text
                      ></view
                    ></view
                  ></view
                ><button
                  v-if="!logReadOnly" :disabled="busy" class="add-set-button"
                  @tap="addLogSet(exercise, group.key)"
                >
                  ＋ 添加{{ group.label }}
                </button></view
              > </view
            ><view class="hint log-save-hint"
              >{{ logReadOnly ? '已归档计划的训练记录只读保留。' : '暂存会保留已勾选组和剩余目标；完成本次训练后才计入进步统计。未勾选的实际输入不会暂存。' }}</view
            ></view
          ></scroll-view
        >
        <view class="log-footer"
          ><button v-if="logReadOnly" class="secondary" @tap="closeLog">关闭记录</button>
          <button v-else-if="logging.status !== 'completed'" class="secondary" :disabled="busy || !completedLogSets" @tap="saveLog(false)">
            暂存进度</button
          ><button
            v-if="!logReadOnly"
            class="primary"
            :disabled="busy || !completedLogSets"
            :loading="busy"
            @tap="saveLog(true)"
          >
            {{ logging.status === 'completed' ? '保存修正记录' : '完成本次训练' }}
          </button></view
        >
      </view></view
    >
  </view>
</template>

<script>
import T1Nav from "../../components/T1Nav.vue";
import {
  trainingApi,
  uploadTrainingMedia,
  resolveTrainingMedia,
} from "../../api/training";
import { hasMemberSession, hasCoachSession } from "../../utils/auth";
import { selectTrainingMedia } from "../../utils/training-media";
import {
  MUSCLE_FILTERS,
  EQUIPMENT_FILTERS,
  searchExercises,
} from "../../utils/exercise-library";

const clone = (value) => JSON.parse(JSON.stringify(value));
const today = () =>
  new Date(Date.now() + 8 * 3600000).toISOString().slice(0, 10);
const uid = () => `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
const confirm = (title, content) =>
  new Promise((resolve) =>
    uni.showModal({
      title,
      content,
      confirmColor: "#ff7a35",
      success: (result) => resolve(result.confirm),
      fail: () => resolve(false),
    }),
  );

export default {
  components: { T1Nav },
  data() {
    return {
      role: "member",
      memberId: "",
      memberContext: null,
      authenticated: false,
      currentDate: today(),
      templates: [],
      plans: [],
      pendingTemplate: "",
      featuredPlan: null,
      previewWorkouts: [],
      selected: null,
      editor: null,
      dayIndex: 0,
      cycleBackup: null,
      extending: false,
      extendDate: today(),
      extendCycles: 1,
      shifting: null,
      shiftDate: today(),
      shiftPreview: null,
      lastProgressSaved: false,
      workouts: [],
      points: [],
      completedWorkouts: 0,
      chartIndex: 0,
      detailTab: "schedule",
      detailTabs: [
        { key: "schedule", label: "训练日程" },
        { key: "progress", label: "我的进步" },
        { key: "outline", label: "计划内容" },
      ],
      scheduleFilters: [
        { key: "pending", label: "待训练" },
        { key: "all", label: "全部" },
        { key: "completed", label: "已完成" },
      ],
      scheduleFilter: "pending",
      scheduleLimit: 20,
      libraryOpen: false,
      libraryPicking: false,
      libraryQuery: "",
      muscleIndex: 0,
      equipmentIndex: 0,
      muscleFilters: MUSCLE_FILTERS,
      equipmentFilters: EQUIPMENT_FILTERS,
      logging: null,
      logSets: [],
      expandedGuides: {},
      mediaViews: {},
      mediaFailed: {},
      setTypes: ["热身", "正式"],
      units: ["kg", "lb"],
      setGroups: [
        { key: "warmup", label: "热身组" },
        { key: "working", label: "正式组" },
      ],
      restPresets: [60, 90, 120],
      restDuration: 90,
      restRemaining: 90,
      restRunning: false,
      restEndAt: 0,
      restFinished: false,
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
      return this.editor && this.editor.days[this.dayIndex];
    },
    canEditPlan() {
      return this.selected && ["draft", "active"].includes(this.selected.status);
    },
    logReadOnly() {
      return this.selected && this.selected.status === "archived";
    },
    pendingWorkoutCount() {
      return this.workouts.filter(w => ["planned", "in_progress"].includes(w.status)).length;
    },
    earliestExtendDate() {
      const last = this.workouts.reduce((date, workout) => workout.plannedDate > date ? workout.plannedDate : date, "");
      return last && this.addDays(last, 1) > this.currentDate ? this.addDays(last, 1) : this.currentDate;
    },
    todayLabel() {
      return `${this.currentDate.slice(5, 7)} 月 ${this.currentDate.slice(8)} 日`;
    },
    focusWorkout() {
      const ordered = [...this.previewWorkouts].sort((a, b) =>
        a.plannedDate.localeCompare(b.plannedDate),
      );
      return (
        ordered.find(
          (w) => w.status === "in_progress",
        ) || ordered.find(
          (w) => w.plannedDate === this.currentDate && w.status === "planned",
        ) ||
        ordered.find(
          (w) => w.plannedDate === this.currentDate && w.status === "rest",
        ) ||
        ordered.find(
          (w) => w.status === "planned" && w.plannedDate < this.currentDate,
        ) ||
        ordered.find(
          (w) => w.status === "planned" && w.plannedDate > this.currentDate,
        ) ||
        null
      );
    },
    focusLabel() {
      if (!this.focusWorkout) return "下一次进步，从计划开始";
      if (this.focusWorkout.status === "in_progress") return "继续上次暂存的训练";
      if (this.focusWorkout.plannedDate === this.currentDate)
        return this.focusWorkout.status === "rest" ? "今日休息" : "今日训练";
      return this.focusWorkout.plannedDate < this.currentDate
        ? "待补练的安排"
        : "下一次训练";
    },
    filteredWorkouts() {
      return this.workouts.filter(
        (w) =>
          this.scheduleFilter === "all" ||
          (this.scheduleFilter === "completed"
            ? w.status === "completed"
            : ["planned", "in_progress"].includes(w.status)),
      );
    },
    visibleWorkouts() {
      return this.filteredWorkouts.slice(0, this.scheduleLimit);
    },
    startTotalDays() {
      return this.selected
        ? (Number(this.cycles) || 0) * this.selected.cycleDays
        : 0;
    },
    chartExercises() {
      const seen = new Set();
      return this.points
        .filter((p) => {
          if (seen.has(p.exerciseKey)) return false;
          seen.add(p.exerciseKey);
          return true;
        })
        .map((p) => ({ key: p.exerciseKey, name: p.exerciseName }));
    },
    chartPoints() {
      const exercise = this.chartExercises[this.chartIndex];
      return exercise
        ? this.points.filter((p) => p.exerciseKey === exercise.key).slice(-30)
        : [];
    },
    chartMax() {
      return Math.max(
        1,
        ...this.chartPoints.map((p) => Number(p.maxWorkingWeightKg || 0)),
      );
    },
    libraryResults() {
      return searchExercises(
        this.libraryQuery,
        this.muscleFilters[this.muscleIndex],
        this.equipmentFilters[this.equipmentIndex],
      );
    },
    completedLogSets() {
      return this.logSets.filter((s) => s.done).length;
    },
    logProgress() {
      return this.logSets.length
        ? Math.round((this.completedLogSets / this.logSets.length) * 100)
        : 0;
    },
    restDisplay() {
      return `${this.pad(Math.floor(this.restRemaining / 60))}:${this.pad(this.restRemaining % 60)}`;
    },
  },
  watch: {
    currentDay(day) {
      if (day) this.warmMedia([day]);
    },
  },
  onLoad(options) {
    this.role = options.role === "coach" ? "coach" : "member";
    this.memberId = options.memberId || "";
    this.pendingTemplate = ["three-split", "five-split"].includes(
      options.template,
    )
      ? options.template
      : "";
    if (this.role === "coach") this.detailTabs[1].label = "学员进步";
    this.authenticated =
      this.role === "coach" ? hasCoachSession() : hasMemberSession();
    this._mediaLoading = {};
    this._loaded = true;
    this.load();
  },
  onShow() {
    this.currentDate = today();
    if (this.restRunning) this.resumeTicker();
    const hasSession =
      this.role === "coach" ? hasCoachSession() : hasMemberSession();
    if (this._loaded && hasSession && !this.authenticated) {
      this.authenticated = true;
      this.load();
    } else if (this._loaded && !hasSession && this.authenticated) {
      this.authenticated = false;
      this.selected = null;
      this.editor = null;
      this.logging = null;
      this.plans = [];
      this.workouts = [];
      this.points = [];
      this.previewWorkouts = [];
      this.resetRestTimer();
    }
  },
  onHide() {
    this.stopTicker();
  },
  onUnload() {
    this.stopTicker();
    this._disposed = true;
    // #ifdef H5
    Object.values(this.mediaViews).forEach((url) => {
      if (typeof url === "string" && url.startsWith("blob:"))
        URL.revokeObjectURL(url);
    });
    // #endif
    this.mediaViews = {};
    this._localPreviews?.clear();
  },
  onBackPress() {
    if (this.busy) return true;
    if (this.libraryOpen) {
      this.libraryOpen = false;
      return true;
    }
    if (this.shifting) { this.shifting = null; return true; }
    if (this.logging) {
      this.closeLog();
      return true;
    }
    if (this.editor) {
      this.cancelEditor();
      return true;
    }
    if (this.selected) {
      this.backToPlans();
      return true;
    }
    return false;
  },
  methods: {
    api(path, method = "GET", data) {
      return trainingApi(path, method, data, this.role);
    },
    toast(title) {
      uni.showToast({ title, icon: "none" });
    },
    pad(value) {
      return String(value).padStart(2, "0");
    },
    shortDate(value) {
      return value
        ? `${Number(value.slice(5, 7))} 月 ${Number(value.slice(8))} 日`
        : "";
    },
    trainingDays(plan) {
      return (plan.days || []).filter((day) => !day.restDay).length;
    },
    exerciseSummary(workout) {
      return `${(workout.exercises || []).length} 个动作 · ${(workout.exercises || []).reduce((sum, ex) => sum + (ex.targets || []).length, 0)} 组目标`;
    },
    targetSummary(exercise) {
      const sets = exercise.targets || [];
      return `热身 ${sets.filter((s) => s.setType === "warmup").length} 组 · 正式 ${sets.filter((s) => s.setType === "working").length} 组`;
    },
    statusLabel(workout) {
      if (workout.status === "in_progress") return "已暂存 · 待完成";
      if (workout.status === "cancelled") return "已停止";
      return workout.status === "completed"
        ? "已完成"
        : workout.status === "rest"
          ? "休息"
          : workout.plannedDate < this.currentDate
            ? "待补练"
            : "待训练";
    },
    planStatus(plan) {
      return { draft: "待开启", active: "进行中", completed: "已完成", archived: "已归档" }[plan.status] || plan.status;
    },
    addDays(date, count) {
      return new Date(Date.parse(date + "T00:00:00Z") + count * 86400000).toISOString().slice(0, 10);
    },
    formatWeight(value) {
      return Number(Number(value || 0).toFixed(2));
    },
    barWidth(point) {
      return Number(point.maxWorkingWeightKg || 0) > 0
        ? Math.max(2, (Number(point.maxWorkingWeightKg) / this.chartMax) * 100)
        : 0;
    },
    goLogin() {
      const query = [];
      if (this.role === "coach") query.push("role=coach");
      if (this.memberId)
        query.push(`memberId=${encodeURIComponent(this.memberId)}`);
      if (this.pendingTemplate) query.push(`template=${this.pendingTemplate}`);
      const target = `/pages/training/index${query.length ? "?" + query.join("&") : ""}`;
      uni.navigateTo({
        url: `/pages/portal/index?role=${this.role}&target=${encodeURIComponent(target)}`,
      });
    },
    backToStudents() {
      uni.reLaunch({ url: "/pages/coach/student/index" });
    },
    async act(fn) {
      if (this.busy) return;
      this.busy = true;
      try {
        return await fn();
      } catch (error) {
        this.toast(error.message || "操作没有完成，请重试");
      } finally {
        this.busy = false;
      }
    },
    async load() {
      if (!this.authenticated || (this.role === "coach" && !this.memberId))
        return;
      this.loading = true;
      this.error = "";
      try {
        const [templates, plans, context] = await Promise.all([
          this.api("/templates"),
          this.api(
            "/plans",
            "GET",
            this.memberId ? { memberId: this.memberId } : {},
          ),
          this.api("/context", "GET", this.memberId ? { memberId: this.memberId } : {}),
        ]);
        this.templates = templates || [];
        this.plans = plans || [];
        this.memberContext = context || null;
        this.featuredPlan =
          this.plans.find((plan) => plan.status === "active") ||
          this.plans[0] ||
          null;
        this.previewWorkouts =
          this.featuredPlan && this.featuredPlan.status === "active"
            ? (await this.api(`/plans/${this.featuredPlan.planId}/workouts`)) ||
              []
            : [];
        if (this.pendingTemplate) {
          const template = this.templates.find(
            (item) => item.templateId === this.pendingTemplate,
          );
          this.pendingTemplate = "";
          if (template) await this.copyTemplate(template);
        }
      } catch (error) {
        this.error = error.message || "训练安排暂时没有读取成功";
      } finally {
        this.loading = false;
      }
    },
    async backToPlans() {
      this.selected = null;
      this.workouts = [];
      this.points = [];
      await this.load();
    },
    async openFocus() {
      const workout = this.focusWorkout;
      const opened = await this.openPlan(this.featuredPlan);
      if (
        opened &&
        workout &&
        workout.status !== "rest" &&
        workout.plannedDate <= this.currentDate
      )
        await this.openLog(workout);
    },
    async openPlan(plan) {
      return this.act(async () => {
        this.selected = await this.api(`/plans/${plan.planId}`);
        this.extending = false;
        this.detailTab = "schedule";
        this.scheduleFilter = "pending";
        this.scheduleLimit = 20;
        await this.refreshPlan();
        uni.pageScrollTo({ scrollTop: 0, duration: 150 });
        return true;
      });
    },
    async refreshPlan() {
      const [workouts, stats] = await Promise.all([
        this.api(`/plans/${this.selected.planId}/workouts`),
        this.api(`/plans/${this.selected.planId}/stats`),
      ]);
      this.workouts = (workouts || []).sort(
        (a, b) =>
          a.plannedDate.localeCompare(b.plannedDate) ||
          a.sequenceNumber - b.sequenceNumber,
      );
      this.points = ((stats && stats.points) || []).sort((a, b) =>
        String(a.date).localeCompare(String(b.date)),
      );
      this.completedWorkouts = (stats && stats.completedWorkouts) || 0;
      this.chartIndex = Math.min(
        this.chartIndex,
        Math.max(0, this.chartExercises.length - 1),
      );
    },
    setScheduleFilter(key) {
      this.scheduleFilter = key;
      this.scheduleLimit = 20;
    },
    emptySet(type = "working") {
      return { setType: type, reps: 10, weight: "", unit: "kg", rpe: "" };
    },
    newPlan() {
      this.cycleBackup = null;
      this.editor = {
        name: "",
        description: "",
        visibility: "private",
        cycleDays: 7,
        days: [],
      };
      this.dayIndex = 0;
      this.resizeDays();
      this.currentDay.restDay = false;
      this.currentDay.title = "训练日";
      uni.pageScrollTo({ scrollTop: 0, duration: 150 });
    },
    normalizeEditor(plan) {
      const result = clone(plan);
      (result.days || []).forEach((day) => {
        day.exercises = day.exercises || [];
        day.exercises.forEach((ex) => {
          ex.imageUrls = ex.imageUrls || [];
          ex.videoUrl = ex.videoUrl || "";
          ex.targets = ex.targets || [this.emptySet()];
        });
      });
      return result;
    },
    async copyTemplate(template) {
      await this.act(async () => {
        const full = template.days
          ? template
          : await this.api(`/plans/${template.planId}`);
        this.editor = this.normalizeEditor(full);
        this.cycleBackup = null;
        delete this.editor.planId;
        delete this.editor.templateId;
        delete this.editor.status;
        this.editor.visibility = "private";
        this.editor.name +=
          this.role === "coach" ? " · 学员定制" : " · 我的版本";
        this.dayIndex = 0;
        this.warmMedia([this.currentDay]);
        uni.pageScrollTo({ scrollTop: 0, duration: 150 });
      });
    },
    editSelected() {
      if (!this.canEditPlan) return;
      this.cycleBackup = null;
      this.editor = this.normalizeEditor(this.selected);
      this.dayIndex = 0;
      this.warmMedia([this.currentDay]);
      uni.pageScrollTo({ scrollTop: 0, duration: 150 });
    },
    async cancelEditor() {
      if (this.busy) return;
      if (
        await confirm(
          "离开编辑？",
          "未保存的修改会丢失。已经保存的计划仍会保留。",
        )
      ) {
        this.editor = null;
        if (!this.selected) await this.load();
      }
    },
    resizeDays() {
      const count = Number(this.editor.cycleDays);
      if (!Number.isInteger(count) || count < 1 || count > 31) {
        this.toast("循环天数需要在 1–31 天之间");
        return false;
      }
      const days = this.editor.days;
      if (count < days.length) {
        this.toast("缩短循环会移除日期，请先确认变更");
        return false;
      }
      this.editor.days = Array.from(
        { length: count },
        (_, index) =>
          days[index] || {
            dayNumber: index + 1,
            title: `第 ${index + 1} 天`,
            restDay: true,
            exercises: [],
          },
      );
      this.dayIndex = Math.min(this.dayIndex, count - 1);
      return true;
    },
    async changeCycleDays() {
      if (!this.editor || this.busy) return;
      const editor = this.editor;
      const count = Number(editor.cycleDays);
      const previousCount = editor.days.length;
      if (!Number.isInteger(count) || count < 1 || count > 31) {
        editor.cycleDays = previousCount; this.toast("循环天数需要在 1–31 天之间"); return;
      }
      if (count >= previousCount) { this.resizeDays(); return; }
      editor.cycleDays = previousCount;
      this.busy = true;
      try {
        const affected = editor.days.slice(count);
        const accepted = await confirm("缩短循环？", `第 ${count + 1}–${previousCount} 天的 ${affected.reduce((sum, day) => sum + day.exercises.length, 0)} 个动作将移出计划。保存前可以恢复。`);
        if (!accepted || this.editor !== editor) return;
        this.cycleBackup = clone(editor.days);
        editor.days = editor.days.slice(0, count); editor.cycleDays = count;
        this.dayIndex = Math.min(this.dayIndex, count - 1);
      } finally { this.busy = false; }
    },
    restoreCycleDays() {
      if (!this.cycleBackup || this.busy) return;
      const current = this.editor.days;
      this.editor.days = this.cycleBackup.map((day, index) => current[index] || clone(day));
      this.editor.cycleDays = this.editor.days.length; this.cycleBackup = null;
      this.toast("已恢复移出的日期，现有日期的修改保留");
    },
    async toggleRest(event) {
      const day = this.currentDay;
      const rest = event.detail.value;
      if (
        rest &&
        day.exercises.length &&
        !(await confirm(
          "改为休息日？",
          "这一天的动作和目标组会从计划中移除，已经完成的训练记录不变。",
        ))
      ) {
        day.restDay = true;
        this.$nextTick(() => {
          day.restDay = false;
        });
        return;
      }
      day.restDay = rest;
      if (rest) day.exercises = [];
    },
    openLibrary(picking) {
      this.libraryPicking = picking;
      this.libraryOpen = true;
    },
    resetLibrary() {
      this.libraryQuery = "";
      this.muscleIndex = 0;
      this.equipmentIndex = 0;
    },
    addExercise(item) {
      if (!this.currentDay) return;
      if (this.currentDay.exercises.length >= 30)
        return this.toast("一天最多安排 30 个动作");
      if (item && this.currentDay.exercises.some((ex) => ex.key === item.key))
        return this.toast("今天已经安排了这个动作");
      this.currentDay.restDay = false;
      this.currentDay.exercises.push({
        key: item ? item.key : `ex-${uid()}`,
        name: item ? item.name : "",
        instructions: item ? item.instructions : "",
        imageUrls: [],
        videoUrl: "",
        targets: [this.emptySet()],
      });
      this.libraryOpen = false;
      this.toast(item ? `已添加${item.name}` : "已添加自定义动作");
    },
    async removeExercise(index) {
      if (
        await confirm(
          "移除这个动作？",
          "这一天的动作目标会一起移除，保存后生效。",
        )
      )
        this.currentDay.exercises.splice(index, 1);
    },
    addTarget(exercise, type) {
      if (exercise.targets.length >= 30)
        return this.toast("每个动作最多 30 组");
      exercise.targets.push(this.emptySet(type));
    },
    removeTarget(exercise, index) {
      if (exercise.targets.length <= 1)
        return this.toast("每个动作至少保留一组目标");
      exercise.targets.splice(index, 1);
    },
    validateSet(set, actual) {
      if (
        !Number.isInteger(Number(set.reps)) ||
        Number(set.reps) < 1 ||
        Number(set.reps) > 1000
      )
        throw new Error("每组次数需要在 1–1000 之间");
      const noWeight = set.weight === "" || set.weight == null;
      if (actual && noWeight) throw new Error("请填写实际重量，自重动作填 0");
      if (
        !noWeight &&
        (!Number.isFinite(Number(set.weight)) ||
          Number(set.weight) < 0 ||
          Number(set.weight) > 2000)
      )
        throw new Error("重量需要在 0–2000 之间");
      if (
        !noWeight &&
        Number(Number(set.weight).toFixed(4)) !== Number(set.weight)
      )
        throw new Error("重量最多保留 4 位小数");
      if (
        set.rpe !== "" &&
        set.rpe != null &&
        (!Number.isFinite(Number(set.rpe)) ||
          Number(set.rpe) < 1 ||
          Number(set.rpe) > 10)
      )
        throw new Error("强度 RPE 选填，范围为 1–10");
      return {
        setType: set.setType,
        reps: Number(set.reps),
        weight: noWeight ? null : Number(set.weight),
        unit: set.unit,
        rpe: set.rpe === "" || set.rpe == null ? null : Number(set.rpe),
      };
    },
    planPayload() {
      if (!this.resizeDays()) throw new Error("请检查循环天数");
      const plan = this.editor;
      if (!String(plan.name || "").trim()) throw new Error("先给计划起个名字");
      if (!plan.days.some((day) => !day.restDay && day.exercises.length))
        throw new Error("请至少安排一个训练日和一个动作");
      if (plan.days.some((day) => !day.restDay && !day.exercises.length))
        throw new Error("训练日需要至少一个动作，空白日期可设为休息日");
      return {
        memberId:
          this.memberId || (this.role === "coach" ? plan.memberId : undefined),
        name: plan.name.trim(),
        description: plan.description || "",
        cycleDays: Number(plan.cycleDays),
        visibility: plan.visibility,
        days: plan.days.map((day) => ({
          dayNumber: day.dayNumber,
          title: day.title || `第 ${day.dayNumber} 天`,
          restDay: day.restDay,
          exercises: day.restDay
            ? []
            : day.exercises.map((ex) => {
                if (!ex.name.trim()) throw new Error("请填写每个动作的名称");
                if (!ex.targets.length)
                  throw new Error("每个动作至少需要一组目标");
                return {
                  key: ex.key,
                  name: ex.name.trim(),
                  instructions: ex.instructions || "",
                  imageUrls: (ex.imageUrls || []).filter(Boolean),
                  videoUrl: ex.videoUrl || "",
                  targets: ex.targets.map((set) =>
                    this.validateSet(set, false),
                  ),
                };
              }),
        })),
      };
    },
    async persistEditor() {
      const editor = this.editor;
      const id = editor.planId;
      const result = await this.api(
        id ? `/plans/${id}` : "/plans",
        id ? "PUT" : "POST",
        this.planPayload(),
      );
      editor.planId = result.planId;
      editor.status = result.status;
      return result;
    },
    savePlan() {
      return this.act(async () => {
        const saved = await this.persistEditor();
        this.editor = null;
        this.cycleBackup = null;
        this.selected = saved;
        this.detailTab = saved.status === "active" ? "schedule" : "outline";
        await this.refreshPlan();
        this.toast("计划已保存");
        uni.pageScrollTo({ scrollTop: 0, duration: 150 });
      });
    },
    copySelected() {
      return this.act(async () => {
        const copy = await this.api(`/plans/${this.selected.planId}/copy`, "POST", {});
        this.selected = copy; this.editor = this.normalizeEditor(copy); this.cycleBackup = null; this.dayIndex = 0;
        this.workouts = []; this.points = []; this.completedWorkouts = 0;
        this.toast("已复制为私人草稿，可调整后开启");
      });
    },
    showExtend() { this.extendDate = this.earliestExtendDate; this.extendCycles = 1; this.extending = !this.extending; },
    extendPlan() {
      return this.act(async () => {
        const cycles = Number(this.extendCycles);
        if (!Number.isInteger(cycles) || cycles < 1 || cycles * this.selected.cycleDays > 366) throw new Error("每次续排 1–366 天，请调整循环次数");
        await this.api(`/plans/${this.selected.planId}/extend`, "POST", { startDate: this.extendDate, cycles });
        this.selected = await this.api(`/plans/${this.selected.planId}`); this.extending = false;
        await this.refreshPlan(); this.detailTab = "schedule"; this.toast("新一轮日程已续排，历史记录保留");
      });
    },
    async completePlan() {
      if (!await confirm("完成这份计划？", "已完成记录会保留，之后可以继续续排循环或复制新计划。")) return;
      this.act(async () => { this.selected = await this.api(`/plans/${this.selected.planId}/complete`, "POST"); this.toast("计划已完成"); });
    },
    async archivePlan() {
      if (this.workouts.some(w => w.status === "in_progress")) return this.toast("请先完成已暂存的训练，再归档计划");
      if (!await confirm("归档并停止后续安排？", "已完成训练、重量和行为记录全部保留。未开始的日程会停止；需要重新使用时可复制为新计划。")) return;
      this.act(async () => { this.selected = await this.api(`/plans/${this.selected.planId}/archive`, "POST"); this.extending = false; await this.refreshPlan(); this.toast("计划已归档，历史记录保留"); });
    },
    startPlan() {
      return this.act(async () => {
        if (
          !Number.isInteger(Number(this.cycles)) ||
          Number(this.cycles) < 1 ||
          this.startTotalDays > 366
        )
          throw new Error(
            `循环次数需要在 1–${Math.floor(366 / this.selected.cycleDays)} 之间`,
          );
        await this.api(`/plans/${this.selected.planId}/start`, "POST", {
          startDate: this.startDate,
          cycles: Number(this.cycles),
        });
        this.selected = await this.api(`/plans/${this.selected.planId}`);
        this.detailTab = "schedule";
        await this.refreshPlan();
        this.toast("训练日程已安排");
      });
    },
    async shift(workout) {
      this.shifting = workout; this.shiftDate = this.addDays(workout.plannedDate, 1); this.shiftPreview = null;
      await this.previewShift();
    },
    async previewShift() {
      await this.act(async () => {
        const days = Math.round((Date.parse(this.shiftDate) - Date.parse(this.shifting.plannedDate)) / 86400000);
        if (days < 1 || days > 365) throw new Error("请选择原日期之后 1–365 天内的日期");
        this.shiftPreview = await this.api(`/plans/${this.selected.planId}/shift/preview`, "GET", { fromDate: this.shifting.plannedDate, days });
      });
    },
    applyShift() {
      if (!this.shiftPreview || !this.shiftPreview.canApply) return;
      return this.act(async () => {
        const preview = this.shiftPreview;
        await this.api(`/plans/${this.selected.planId}/shift`, "POST", { fromDate: preview.fromDate, days: preview.days, expectedVersion: preview.version });
        this.shifting = null; this.shiftPreview = null; await this.refreshPlan(); this.toast("日程已按预览顺延");
      });
    },
    ensureMedia(url) {
      if (!url || this.mediaViews[url])
        return Promise.resolve(this.mediaViews[url]);
      this._mediaLoading = this._mediaLoading || {};
      if (this._mediaLoading[url]) return this._mediaLoading[url];
      this._mediaLoading[url] = resolveTrainingMedia(url, this.role)
        .then((result) => {
          if (this._disposed) {
            // #ifdef H5
            if (result && result.startsWith("blob:"))
              URL.revokeObjectURL(result);
            // #endif
            return "";
          }
          this.mediaViews = { ...this.mediaViews, [url]: result };
          this.mediaFailed = { ...this.mediaFailed, [url]: false };
          return result;
        })
        .catch(() => {
          this.mediaFailed = { ...this.mediaFailed, [url]: true };
          return "";
        })
        .finally(() => {
          delete this._mediaLoading[url];
        });
      return this._mediaLoading[url];
    },
    warmMedia(days) {
      (days || []).forEach((day) =>
        (day.exercises || []).forEach((ex) => {
          (ex.imageUrls || []).forEach((url) => this.ensureMedia(url));
        }),
      );
    },
    retryMedia(url) {
      this.mediaFailed = { ...this.mediaFailed, [url]: false };
      this.ensureMedia(url);
    },
    mediaPreviewFailed(url) {
      const current = this.mediaViews[url];
      if (!current || this._disposed) return;
      // #ifdef H5
      if (current.startsWith("blob:")) URL.revokeObjectURL(current);
      // #endif
      this.mediaViews = { ...this.mediaViews, [url]: "" };
      if (this._localPreviews?.delete(url)) return this.ensureMedia(url);
      this.mediaFailed = { ...this.mediaFailed, [url]: true };
    },
    previewMedia(url, urls) {
      const current = this.mediaViews[url];
      if (current)
        uni.previewImage({
          current,
          urls: urls.map((item) => this.mediaViews[item]).filter(Boolean),
        });
    },
    upload(exercise, type) {
      if (this.busy) return;
      if (type === "image" && exercise.imageUrls.length >= 9)
        return this.toast("每个动作最多添加 9 张图片");
      return this.act(async () => {
        const selectedFile = await selectTrainingMedia(type);
        if (!selectedFile) return;
        if (!this.editor.planId) await this.persistEditor();
        const result = await uploadTrainingMedia(selectedFile, this.editor.planId, this.role);
        if (this._disposed) return;
        if (type === "image") exercise.imageUrls.push(result.url);
        else exercise.videoUrl = result.url;
        try {
          const preview = typeof selectedFile === "string" ? selectedFile : URL.createObjectURL(selectedFile);
          this._localPreviews ||= new Set();
          this._localPreviews.add(result.url);
          this.mediaViews = { ...this.mediaViews, [result.url]: preview };
        } catch {
          this.ensureMedia(result.url);
        }
        this.toast("素材已上传，记得保存计划");
      });
    },
    async openLog(workout) {
      if (this.busy) return;
      if (this.selected && this.selected.startDate > this.currentDate)
        return this.toast("计划尚未开始，可以先查看动作与目标");
      this.logging = clone(workout);
      this.performedDate = workout.performedDate || today();
      this.expandedGuides = {};
      this.logging.exercises = (this.logging.exercises || []).map((ex) => ({
        ...ex,
        imageUrls: ex.imageUrls || [],
        videoUrl: ex.videoUrl || "",
      }));
      this.logSets = this.restoreLogSets(workout);
      this.lastProgressSaved = false;
      this.warmMedia([{ exercises: this.logging.exercises }]);
    },
    restoreLogSets(workout) {
      const saved = clone(workout.sets || []).map(set => ({ ...set, localId: uid(), done: true }));
      if (workout.status === "completed" || this.logReadOnly) return saved;
      const matched = new Set();
      const targets = (workout.exercises || []).flatMap(ex => (ex.targets || []).map((target, targetSetIndex) => {
        const index = saved.findIndex(set => set.exerciseKey === ex.key && set.targetSetIndex === targetSetIndex);
        if (index >= 0) { matched.add(index); return { ...saved[index], targetWeight: target.weight, targetUnit: target.unit }; }
        return { ...clone(target), localId: uid(), done: false, targetSetIndex, targetWeight: target.weight,
          targetUnit: target.unit, weight: "", rpe: "", exerciseKey: ex.key, exerciseName: ex.name };
      }));
      return targets.concat(saved.filter((set, index) => !matched.has(index)));
    },
    toggleGuide(key) {
      this.expandedGuides = {
        ...this.expandedGuides,
        [key]: !this.expandedGuides[key],
      };
    },
    previousWorkout(exercise) {
      return this.workouts
        .filter(
          (w) =>
            w.status === "completed" &&
            w.workoutId !== this.logging.workoutId &&
            (w.performedDate || w.plannedDate) <= this.performedDate &&
            (w.sets || []).some(
              (set) =>
                set.exerciseKey === exercise.key && set.setType === "working",
            ),
        )
        .sort(
          (a, b) =>
            (b.performedDate || b.plannedDate).localeCompare(
              a.performedDate || a.plannedDate,
            ) || b.sequenceNumber - a.sequenceNumber,
        )[0];
    },
    previousFor(exercise) {
      const workout = this.previousWorkout(exercise);
      return workout
        ? workout.sets.filter(
            (set) =>
              set.exerciseKey === exercise.key && set.setType === "working",
          )
        : [];
    },
    previousDate(exercise) {
      const workout = this.previousWorkout(exercise);
      return workout ? workout.performedDate || workout.plannedDate : "";
    },
    previousSummary(exercise) {
      const sets = this.previousFor(exercise);
      return (
        sets
          .slice(0, 3)
          .map(
            (set) =>
              `${this.formatWeight(set.weight)} ${set.unit} × ${set.reps}`,
          )
          .join(" / ") + (sets.length > 3 ? ` …共 ${sets.length} 组` : "")
      );
    },
    async usePrevious(exercise) {
      if (this.logReadOnly || this.busy) return;
      const prior = this.previousFor(exercise);
      if (!prior.length) return;
      const rows = this.logSets.filter(
        (set) =>
          set.exerciseKey === exercise.key &&
          set.setType === "working" &&
          !set.done,
      );
      if (!rows.length) return this.toast("没有待填写的正式组");
      if (
        rows.some((set) => set.weight !== "" && set.weight != null) &&
        !(await confirm(
          "替换未完成组的数据？",
          "将上次的正式组重量、次数和单位填入，仍需本次完成后逐组确认。",
        ))
      )
        return;
      rows.forEach((set, index) => {
        const value = prior[Math.min(index, prior.length - 1)];
        set.weight = value.weight;
        set.reps = value.reps;
        set.unit = value.unit;
      });
      this.toast("已填入上次数据，完成后点勾确认");
    },
    setNumber(index) {
      const item = this.logSets[index];
      return this.logSets
        .slice(0, index + 1)
        .filter(
          (set) =>
            set.exerciseKey === item.exerciseKey &&
            set.setType === item.setType,
        ).length;
    },
    addLogSet(exercise, type) {
      if (this.logReadOnly || this.busy) return;
      if (this.logSets.length >= 300)
        return this.toast("一次训练最多记录 300 组");
      this.logSets.push({
        ...this.emptySet(type),
        localId: uid(),
        exerciseKey: exercise.key,
        exerciseName: exercise.name,
        done: false,
      });
    },
    async removeLogSet(index) {
      if (this.logReadOnly || this.busy) return;
      if (
        !this.logSets[index].done ||
        (await confirm(
          "移除已完成的一组？",
          "保存后，这组数据会从本次训练记录中移除。",
        ))
      )
        this.logSets.splice(index, 1);
    },
    completeSet(set) {
      if (this.logReadOnly || this.busy) return;
      if (set.done) {
        set.done = false;
        return;
      }
      try {
        this.validateSet(set, true);
        set.done = true;
        this.startRestTimer();
      } catch (error) {
        this.toast(error.message);
      }
    },
    async closeLog() {
      if (this.busy) return;
      if (this.logReadOnly) { this.logging = null; return; }
      if (
        !(await confirm(
          "离开本次记录？",
          "本次尚未保存的修改不会保留。已经保存的训练记录不受影响。",
        ))
      )
        return;
      this.logging = null;
      this.resetRestTimer();
    },
    async saveLog(finish = true) {
      if (this.logReadOnly || this.busy) return;
      const done = this.logSets.filter((set) => set.done);
      if (!done.length) return this.toast("先完成并勾选至少一组");
      if (
        finish && done.length < this.logSets.length &&
        !(await confirm(
          "结束本次训练？",
          `仅将已勾选的 ${done.length} 组计入已完成训练，其余 ${this.logSets.length - done.length} 组不会计入。还要继续练时请选择暂存进度。`,
        ))
      )
        return;
      await this.act(async () => {
        const sets = done.map((set) => ({
          ...this.validateSet(set, true),
          exerciseKey: set.exerciseKey,
          exerciseName: set.exerciseName,
          targetSetIndex: set.targetSetIndex == null ? null : set.targetSetIndex,
        }));
        const saved = await this.api(`/workouts/${this.logging.workoutId}/log`, "PUT", {
          performedDate: this.performedDate,
          sets,
          finish,
        });
        if (finish) { this.logging = null; this.resetRestTimer(); }
        else { this.logging.status = "in_progress"; this.logging.sets = saved.sets || sets; this.lastProgressSaved = true; }
        if (this.selected) this.selected = await this.api(`/plans/${this.selected.planId}`);
        await this.refreshPlan();
        this.toast(finish ? "本次训练已完成" : "进度已暂存，可继续练或稍后回来");
      });
    },
    setRestDuration(seconds) {
      this.restDuration = seconds;
      this.resetRestTimer();
    },
    startRestTimer() {
      this.restRemaining = this.restDuration;
      this.restEndAt = Date.now() + this.restDuration * 1000;
      this.restRunning = true;
      this.restFinished = false;
      this.resumeTicker();
    },
    toggleRestTimer() {
      if (this.restRunning) {
        this.tickRest();
        this.restRunning = false;
        this.stopTicker();
      } else {
        if (this.restRemaining <= 0) this.restRemaining = this.restDuration;
        this.restEndAt = Date.now() + this.restRemaining * 1000;
        this.restRunning = true;
        this.restFinished = false;
        this.resumeTicker();
      }
    },
    resetRestTimer() {
      this.stopTicker();
      this.restRunning = false;
      this.restRemaining = this.restDuration;
      this.restEndAt = 0;
      this.restFinished = false;
    },
    tickRest() {
      if (!this.restRunning) return;
      this.restRemaining = Math.max(
        0,
        Math.ceil((this.restEndAt - Date.now()) / 1000),
      );
      if (!this.restRemaining) {
        this.restRunning = false;
        this.restFinished = true;
        this.stopTicker();
        uni.vibrateShort({ fail: () => {} });
      }
    },
    stopTicker() {
      if (this._restTicker) {
        clearInterval(this._restTicker);
        this._restTicker = null;
      }
    },
    resumeTicker() {
      this.stopTicker();
      this.tickRest();
      if (this.restRunning)
        this._restTicker = setInterval(() => this.tickRest(), 250);
    },
  },
};
</script>

<style scoped>
.shift-sheet { height: 85vh; }
.shift-controls { padding-top: 24rpx; padding-bottom: 24rpx; flex-shrink: 0; }
.shift-change { border-bottom: 1px solid var(--t1-border); padding: 20rpx 0; }
.member-context .name { margin: 12rpx 0; }
.lifecycle-actions { flex-wrap: wrap; }
.cycle-warning { border-color: var(--t1-accent); }
.training-page {
  min-height: 100vh;
  background: var(--t1-bg);
  color: var(--t1-text);
  padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  font-size: 28rpx;
  box-sizing: border-box;
}
.page-content {
  max-width: 960px;
  margin: 0 auto;
  padding: 32rpx;
}
.page-heading,
.section-head,
.form-row,
.actions,
.plan-card,
.library-banner,
.sheet-header,
.filter-pickers,
.timer-main,
.timer-presets,
.timer-buttons,
.actual-set-head,
.set-inputs,
.set-extra,
.previous-result {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}
.page-heading {
  margin-bottom: 30rpx;
}
.eyebrow {
  font-size: 20rpx;
  font-weight: 700;
  letter-spacing: 3rpx;
  color: var(--t1-muted);
}
.accent {
  color: var(--t1-accent) !important;
}
.page-title {
  font-size: 48rpx;
  font-weight: 750;
  line-height: 1.4;
  margin-top: 8rpx;
}
.section-title {
  font-size: 34rpx;
  line-height: 1.4;
  font-weight: 700;
}
.name {
  font-size: 29rpx;
  font-weight: 650;
  line-height: 1.5;
  word-break: break-word;
}
.muted {
  font-size: 25rpx;
  color: var(--t1-muted);
  line-height: 1.7;
}
.hint {
  font-size: 22rpx;
  color: var(--t1-muted);
  line-height: 1.6;
}
.space-top {
  margin-top: 24rpx !important;
}
.section-space {
  margin: 40rpx 0 20rpx;
}
.flex-one {
  flex: 1;
  min-width: 0;
}
.card {
  background: var(--t1-surface);
  border: 1px solid var(--t1-border);
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-sizing: border-box;
}
.hero-photo,
.hero-shade {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
.focus-card {
  border-top: 4rpx solid var(--t1-accent);
}
.focus-title {
  font-size: 38rpx;
  font-weight: 700;
  line-height: 1.45;
  margin: 20rpx 0 12rpx;
}
.plan-icon {
  width: 92rpx;
  height: 92rpx;
  border-radius: 16rpx;
  background: var(--t1-inset);
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--t1-accent);
  font-size: 34rpx;
  flex-shrink: 0;
  font-weight: 750;
}
.plan-icon text {
  font-size: 18rpx;
  margin-left: 4rpx;
}
.plan-card {
  padding: 26rpx 24rpx;
  gap: 22rpx;
}
.plan-end {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12rpx;
}
.arrow {
  font-size: 30rpx;
  color: var(--t1-muted);
}
.badge {
  display: inline-block;
  padding: 7rpx 13rpx;
  border: 1px solid var(--t1-border);
  background: var(--t1-inset);
  color: var(--t1-muted);
  border-radius: 8rpx;
  font-size: 20rpx;
  white-space: nowrap;
  font-weight: 500;
  line-height: 1.4;
}
.badge-accent {
  background: var(--t1-tint);
  color: var(--t1-accent);
  border-color: var(--t1-accent);
}
.template-card {
  padding: 28rpx 32rpx;
}
.template-title {
  font-size: 36rpx;
  font-weight: 700;
  margin: 20rpx 0 10rpx;
}
.template-description {
  font-size: 24rpx;
  color: var(--t1-muted);
  line-height: 1.7;
  margin-top: 18rpx;
}
.library-banner {
  padding: 28rpx 8rpx;
  margin-top: 16rpx;
}
.library-arrow {
  font-size: 42rpx;
}
.empty-card {
  text-align: center;
  padding: 40rpx 24rpx;
}
.empty-card .muted {
  margin-top: 10rpx;
}
.back-link {
  color: var(--t1-muted) !important;
  background: transparent !important;
  justify-content: flex-start !important;
  text-align: left !important;
  padding: 0 !important;
  min-height: 72rpx !important;
  margin-bottom: 16rpx !important;
}
.detail-title {
  font-size: 40rpx;
  line-height: 1.4;
  font-weight: 750;
  margin: 18rpx 0;
}
.metrics {
  display: flex;
  justify-content: space-between;
  margin-top: 32rpx;
  padding-top: 28rpx;
  border-top: 1px solid var(--t1-border);
}
.metrics > view {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.metric-value {
  font-size: 44rpx;
  font-weight: 700;
  line-height: 1.3;
}
.metric-unit {
  font-size: 22rpx;
  font-weight: 400;
  color: var(--t1-muted);
  margin-left: 7rpx;
}
.metric-label {
  font-size: 22rpx;
  color: var(--t1-muted);
  margin-top: 8rpx;
}
.start-form {
  margin-top: 30rpx;
  padding-top: 14rpx;
  border-top: 1px solid var(--t1-border);
}
.form-row {
  padding: 18rpx 0;
  gap: 18rpx;
}
.form-row > view:first-child {
  flex: 1;
  min-width: 0;
}
.segmented {
  display: flex;
  padding: 7rpx;
  background: var(--t1-surface);
  border: 1px solid var(--t1-border);
  border-radius: 16rpx;
  margin: 28rpx 0;
}
.segment {
  flex: 1;
  text-align: center;
  padding: 22rpx 4rpx;
  font-size: 26rpx;
  color: var(--t1-muted);
  border-radius: 12rpx;
}
.segment.active {
  background: var(--t1-inset);
  color: var(--t1-text);
  font-weight: 650;
}
.filter-row {
  display: flex;
  gap: 16rpx;
  margin: 24rpx 0;
}
.filter-chip {
  border: 1px solid var(--t1-border);
  border-radius: 12rpx;
  color: var(--t1-muted);
  padding: 17rpx 24rpx;
  font-size: 24rpx;
}
.filter-chip.active {
  border-color: var(--t1-accent);
  background: var(--t1-tint);
  color: var(--t1-accent);
}
.schedule-tip {
  margin-bottom: 24rpx;
}
.workout-card {
  display: flex;
  gap: 22rpx;
  padding: 26rpx 24rpx;
}
.workout-date {
  width: 62rpx;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 2rpx;
}
.date-day {
  font-size: 42rpx;
  font-weight: 700;
  line-height: 1.1;
}
.date-month {
  font-size: 21rpx;
  color: var(--t1-muted);
  margin-top: 7rpx;
}
.workout-today {
  border-color: var(--t1-accent);
}
.workout-today .date-day {
  color: var(--t1-accent);
}
.workout-rest {
  background: var(--t1-inset);
}
.rest-shift {
  padding: 8rpx 0 !important;
  margin-top: 8rpx !important;
  text-align: left !important;
  justify-content: flex-start !important;
}
.workout-card .section-head {
  gap: 10rpx;
  align-items: flex-start;
}
.workout-card .name {
  font-size: 28rpx;
}
.chart-picker {
  display: flex;
  justify-content: space-between;
  margin: 28rpx 0;
}
.chart-entry {
  padding: 24rpx 0;
  border-bottom: 1px solid var(--t1-border);
}
.chart-weight {
  font-size: 34rpx;
  font-weight: 700;
  color: var(--t1-accent);
}
.bar-track {
  height: 12rpx;
  background: var(--t1-inset);
  border-radius: 8rpx;
  overflow: hidden;
  margin: 18rpx 0;
}
.bar {
  height: 100%;
  background: var(--t1-text);
  border-radius: 8rpx;
  transition: width 0.25s;
}
.chart-caption {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8rpx;
  font-size: 21rpx;
  color: var(--t1-muted);
}
.outline-exercise {
  padding-top: 24rpx;
  margin-top: 24rpx;
  border-top: 1px solid var(--t1-border);
}
.guide-copy {
  font-size: 25rpx;
  color: var(--t1-muted);
  line-height: 1.8;
  white-space: pre-line;
  margin-top: 16rpx;
}
.editor-lead {
  margin-bottom: 24rpx;
}
.field-label {
  display: block;
  color: var(--t1-muted);
  font-size: 25rpx;
  margin: 18rpx 0 14rpx;
}
.field-label:first-child {
  margin-top: 0;
}
.horizontal-scroll {
  white-space: nowrap;
  width: 100%;
  margin: 28rpx 0;
}
.day-tabs {
  display: inline-flex;
  gap: 14rpx;
  padding: 2rpx 0 10rpx;
}
.day-tab {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 120rpx;
  max-width: 200rpx;
  box-sizing: border-box;
  padding: 20rpx;
  border: 1px solid var(--t1-border);
  border-radius: 14rpx;
  background: var(--t1-surface);
  font-size: 25rpx;
  gap: 8rpx;
}
.day-caption {
  font-size: 20rpx;
  color: var(--t1-muted);
  max-width: 160rpx;
  overflow: hidden;
  text-overflow: ellipsis;
}
.day-tab.active {
  background: var(--t1-tint);
  border-color: var(--t1-accent);
  color: var(--t1-accent);
}
.day-tab.active .day-caption {
  color: var(--t1-muted);
}
.rest-card {
  text-align: center;
  padding: 48rpx 24rpx;
}
.rest-symbol {
  font-size: 70rpx;
  color: var(--t1-accent);
  margin-bottom: 16rpx;
}
.set-section-heading {
  margin: 26rpx 0 18rpx;
}
.set-section-heading .hint {
  font-size: 20rpx;
}
.target-set {
  padding: 12rpx 18rpx 20rpx;
  background: var(--t1-inset);
  border: 1px solid var(--t1-border);
  border-radius: 14rpx;
  margin-top: 14rpx;
}
.target-set.warmup-set {
  border-left: 4rpx solid var(--t1-border);
}
.set-type {
  padding: 12rpx 0;
  color: var(--t1-accent);
  font-size: 23rpx;
}
.set-type.warmup {
  color: var(--t1-muted);
}
.set-inputs {
  gap: 12rpx;
  align-items: flex-end;
}
.set-field {
  flex: 1;
  min-width: 0;
}
.set-field input {
  height: 80rpx;
  padding: 0 8rpx;
  text-align: center;
}
.input-caption {
  display: block;
  font-size: 20rpx;
  color: var(--t1-muted);
  margin-bottom: 8rpx;
  white-space: nowrap;
}
.unit-field {
  width: 100rpx;
  flex-shrink: 0;
}
.unit-picker {
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  background: var(--t1-inset);
  border: 1px solid var(--t1-border);
  border-radius: 12rpx;
  font-size: 24rpx;
  box-sizing: border-box;
}
.rpe-field {
  flex: 0.8;
}
.edit-exercise > textarea {
  margin-top: 16rpx;
}
.media-section {
  border-top: 1px solid var(--t1-border);
  margin-top: 30rpx;
  padding-top: 24rpx;
}
.media-grid {
  display: flex;
  gap: 14rpx;
  flex-wrap: wrap;
  margin-top: 18rpx;
}
.media-tile {
  position: relative;
  width: calc(33.333% - 10rpx);
  height: 150rpx;
  border-radius: 12rpx;
  background: var(--t1-inset);
  overflow: hidden;
}
.media-tile image {
  height: 100%;
  width: 100%;
}
.media-placeholder {
  height: 100%;
  min-height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 21rpx;
  color: var(--t1-muted);
  line-height: 1.6;
}
.media-remove {
position: absolute !important;
  right: 0;
  top: 0;
  width: 52rpx !important;
  min-height: 52rpx !important;
  height: 52rpx !important;
  line-height: 1 !important;
  font-size: 30rpx !important;
  background: rgba(0, 0, 0, 0.72) !important;
  color: var(--t1-on-dark) !important;
  border-radius: 0 0 0 12rpx !important;
  padding: 0 !important;
}
.guide-video {
  width: 100%;
  height: 340rpx;
  margin-top: 18rpx;
  border-radius: 12rpx;
  overflow: hidden;
  background: var(--t1-bg);
}
.editor-bottom {
  position: sticky;
  bottom: 0;
  z-index: 3;
  display: flex;
  gap: 20rpx;
  background: var(--t1-surface);
  padding: 24rpx 0 calc(24rpx + env(safe-area-inset-bottom));
  margin-top: 24rpx;
  border-top: 1px solid var(--t1-border);
}
.editor-bottom button:first-child {
  flex: 1;
}
.editor-bottom button:last-child {
  flex: 2;
}
.loading-state {
  padding: 64rpx 20rpx;
  text-align: center;
  color: var(--t1-muted);
  font-size: 26rpx;
}
.error-box {
  border: 1px solid var(--t1-danger);
  background: var(--t1-tint);
  padding: 24rpx;
  border-radius: 14rpx;
  color: var(--t1-danger);
  font-size: 25rpx;
  line-height: 1.6;
  margin-bottom: 24rpx;
}
.danger {
  color: var(--t1-danger) !important;
}
input,
textarea {
  box-sizing: border-box;
  background: var(--t1-inset);
  border: 1px solid var(--t1-border);
  border-radius: 12rpx;
  color: var(--t1-text);
  font-size: 27rpx;
  min-width: 0;
  width: 100%;
}
input {
  height: 84rpx;
  padding: 0 18rpx;
}
textarea {
  height: 170rpx;
  padding: 18rpx;
  line-height: 1.6;
  margin-bottom: 16rpx;
}
.input-placeholder {
  color: var(--t1-muted);
  font-size: 25rpx;
}
.number-input {
  width: 114rpx !important;
  flex: 0 0 114rpx !important;
  text-align: center;
}
.picker-box {
  min-height: 78rpx;
  box-sizing: border-box;
  padding: 20rpx;
  background: var(--t1-inset);
  border: 1px solid var(--t1-border);
  border-radius: 12rpx;
  font-size: 24rpx;
  line-height: 1.5;
}
.actions {
  margin-top: 20rpx;
}
.actions button {
  flex: 1;
}
.tag-row {
  display: flex;
  gap: 12rpx;
  margin-top: 12rpx;
}
button {
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  min-height: 88rpx;
  margin: 0;
  padding: 20rpx 24rpx;
  border-radius: 14rpx;
  font-size: 27rpx;
  font-weight: 600;
  line-height: 1.45;
  background: var(--t1-inset);
  color: var(--t1-text);
  white-space: normal;
}
button::after {
  border: 0;
}
.primary {
  background: var(--t1-text);
  color: var(--t1-on-dark);
}
.secondary {
  background: var(--t1-inset);
  color: var(--t1-text);
  border: 1px solid var(--t1-border);
}
.compact {
  min-height: 76rpx;
  font-size: 24rpx;
  padding: 16rpx 14rpx;
}
.text-button {
  background: transparent;
  min-height: 72rpx;
  font-size: 24rpx;
  color: var(--t1-muted);
  padding: 10rpx 4rpx;
  flex-shrink: 0;
}
.remove-button {
  background: transparent;
  width: 64rpx;
  min-height: 64rpx;
  font-size: 32rpx;
  padding: 0;
  color: var(--t1-danger);
}
.close-button {
  width: 76rpx;
  min-height: 76rpx;
  padding: 0;
  background: var(--t1-inset);
  border-radius: 50%;
  font-size: 40rpx;
  font-weight: 400;
  flex-shrink: 0;
}
.sheet-mask {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 100;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: flex-end;
}
.sheet {
  width: 100%;
  max-width: 960px;
  height: 94vh;
  background: var(--t1-inset);
  border: 1px solid var(--t1-border);
  border-radius: 28rpx 28rpx 0 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sheet-header {
  padding: 28rpx 32rpx 22rpx;
  border-bottom: 1px solid var(--t1-border);
  flex-shrink: 0;
}
.sheet-header .section-title {
  margin-top: 10rpx;
  font-size: 32rpx;
}
.library-search {
  padding: 24rpx 32rpx 16rpx;
  flex-shrink: 0;
}
.filter-pickers {
  justify-content: flex-start;
  margin: 18rpx 0 14rpx;
}
.filter-pickers picker {
  flex: 1;
}
.library-scroll,
.log-scroll {
  height: 0;
  flex: 1;
  min-height: 0;
}
.sheet-body {
  padding: 0 32rpx calc(36rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}
.library-exercise {
  padding: 28rpx 0;
  border-bottom: 1px solid var(--t1-border);
}
.library-exercise:last-of-type {
  border-bottom: 0;
}
.add-library-button {
  width: 76rpx;
  min-height: 76rpx;
  padding: 0;
  background: var(--t1-text);
  color: var(--t1-on-dark);
  font-size: 36rpx;
}
.guide-step {
  display: flex;
  gap: 16rpx;
  font-size: 24rpx;
  color: var(--t1-muted);
  line-height: 1.75;
  margin-top: 16rpx;
}
.step-number {
  flex: 0 0 32rpx;
  color: var(--t1-accent);
  font-size: 22rpx;
}
.exercise-cues {
  font-size: 21rpx;
  color: var(--t1-muted);
  padding: 18rpx;
  background: var(--t1-inset);
  border-radius: 10rpx;
  line-height: 1.7;
  margin-top: 20rpx;
}
.log-mask {
  z-index: 90;
}
.log-sheet {
  height: 100%;
  border-radius: 0;
}
.rest-timer {
  padding: 22rpx 32rpx;
  background: var(--t1-inset);
  border-bottom: 1px solid var(--t1-border);
  flex-shrink: 0;
}
.timer-label {
  font-size: 22rpx;
  color: var(--t1-muted);
}
.timer-time {
  font-size: 60rpx;
  font-weight: 700;
  line-height: 1.2;
  color: var(--t1-accent);
  font-variant-numeric: tabular-nums;
  letter-spacing: 2rpx;
}
.timer-controls {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.timer-presets {
  gap: 10rpx;
}
.timer-preset {
  font-size: 21rpx;
  padding: 12rpx 14rpx;
  border: 1px solid var(--t1-border);
  border-radius: 8rpx;
  color: var(--t1-muted);
}
.timer-preset.active {
  border-color: var(--t1-accent);
  color: var(--t1-muted);
  background: var(--t1-tint);
}
.timer-buttons {
  gap: 40rpx;
}
.timer-buttons .text-button {
  min-height: 58rpx;
}
.timer-hint {
  font-size: 20rpx;
  color: var(--t1-muted);
  margin-top: 12rpx;
  line-height: 1.5;
}
.log-progress {
  padding: 24rpx 0;
  border-top: 1px solid var(--t1-border);
}
.log-exercise {
  padding: 32rpx 0;
  border-top: 1px solid var(--t1-border);
}
.log-exercise-name {
  font-size: 36rpx;
  font-weight: 700;
  margin: 16rpx 0 20rpx;
}
.previous-result {
  padding: 20rpx;
  background: var(--t1-inset);
  border-radius: 14rpx;
  margin-bottom: 16rpx;
  align-items: flex-start;
}
.previous-weight {
  font-size: 24rpx;
  color: var(--t1-muted);
  margin-top: 8rpx;
  line-height: 1.6;
}
.instruction-toggle {
  display: flex;
  justify-content: space-between;
  padding: 24rpx 0;
  font-size: 24rpx;
  color: var(--t1-muted);
  min-height: 72rpx;
  box-sizing: border-box;
}
.group-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 24rpx 0 16rpx;
  font-size: 26rpx;
}
.group-heading .hint {
  font-size: 20rpx;
}
.warmup-label {
  color: var(--t1-muted);
}
.actual-set {
  background: var(--t1-inset);
  border: 1px solid var(--t1-border);
  border-radius: 14rpx;
  padding: 0 18rpx 20rpx;
  margin-bottom: 14rpx;
}
.actual-set.set-complete {
  border-color: var(--t1-accent);
  background: var(--t1-inset);
}
.actual-set-head {
  height: 58rpx;
}
.actual-set-head .remove-button {
  min-height: 52rpx;
  height: 52rpx;
}
.complete-set-button {
  width: 76rpx;
  min-height: 80rpx;
  height: 80rpx;
  padding: 0;
  background: var(--t1-inset);
  border: 1px solid var(--t1-border);
  color: var(--t1-muted);
  font-size: 36rpx;
  flex-shrink: 0;
}
.complete-set-button.checked {
  background: var(--t1-text);
  color: var(--t1-on-dark);
  border-color: var(--t1-accent);
}
.set-extra {
  margin-top: 18rpx;
  justify-content: flex-start;
  gap: 12rpx;
  flex-wrap: wrap;
}
.set-extra input {
  height: 60rpx;
  width: 150rpx;
  font-size: 22rpx;
  padding: 0 10rpx;
}
.set-extra .input-placeholder {
  font-size: 21rpx;
}
.target-reference {
  margin-left: auto;
}
.add-set-button {
  width: 100%;
  min-height: 74rpx;
  border: 1px dashed var(--t1-border);
  background: transparent;
  color: var(--t1-muted);
  font-size: 24rpx;
}
.log-footer {
  display: flex;
  gap: 18rpx;
  flex-shrink: 0;
  padding: 22rpx 32rpx calc(22rpx + env(safe-area-inset-bottom));
  border-top: 1px solid var(--t1-border);
  background: var(--t1-inset);
}
.log-footer button:first-child {
  flex: 1;
  font-size: 24rpx;
}
.log-footer button:last-child {
  flex: 2;
  font-size: 25rpx;
}
.log-save-hint {
  padding: 10rpx 0 0;
}
.log-body {
  padding-bottom: 32rpx;
}
@media (min-width: 768px) {
  .page-content {
    padding: 40px 32px;
  }
  .card {
    padding: 28px;
  }
  .sheet {
    max-width: 760px;
  }
  .log-sheet {
    height: 96vh;
    border-radius: 24px 24px 0 0;
  }
  .media-tile {
    height: 180px;
  }
  .plan-card {
    padding: 24px;
  }
  .template-card {
    padding: 28px;
  }
  .workout-card {
    padding: 24px;
  }
  .editor-bottom {
    padding-bottom: 24px;
  }
}
</style>
