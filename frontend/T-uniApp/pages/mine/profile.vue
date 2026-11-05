<template>
  <view class="profile-page">
    <view class="page-title">{{ onboarding ? '完善会员资料' : '个人资料' }}</view>
    <view v-if="onboarding" class="intro">以下资料均为选填，以后也可以在「我的」中修改。</view>

    <view v-if="loading" class="notice">正在读取资料…</view>
    <view v-else-if="loadError" class="notice">
      <text>{{ loadError }}</text>
      <button class="text-button" @tap="loadProfile">重新读取</button>
    </view>
    <template v-else>
      <view class="avatar-row">
        <MemberAvatar :source="avatarFile || form.avatarUrl" />
        <view class="avatar-actions">
          <button class="avatar-button" :disabled="busy || choosing" @tap="chooseAvatar">
            {{ avatarFile || form.avatarUrl ? '更换头像' : '添加头像' }}
          </button>
          <text class="hint">JPG 或 PNG，最大 5 MB</text>
        </view>
        <button v-if="avatarFile || form.avatarUrl" class="text-button remove" :disabled="busy || choosing" @tap="removeAvatar">移除</button>
      </view>

      <view class="form-card">
        <view class="field">
          <text class="label">昵称</text>
          <input v-model="form.nickname" :disabled="busy" class="input" type="text" maxlength="20" placeholder="你希望被怎样称呼" />
        </view>
        <view class="field">
          <text class="label">联系手机号</text>
          <input v-model="form.contactMobile" :disabled="busy" class="input" type="text" maxlength="16" placeholder="选填" />
        </view>
        <view class="field-note">供门店联系使用。认领已有会员档案，可前往「我的 → 账号与登录」绑定手机号。</view>
        <template v-if="!onboarding">
          <view v-if="form.mobile" class="field">
            <text class="label">已绑定手机号</text><text class="value">{{ form.mobile }}</text>
          </view>
          <view class="field">
            <text class="label">姓名</text>
            <input v-model="form.realName" :disabled="busy" class="input" maxlength="20" placeholder="选填" />
          </view>
          <picker :disabled="busy" :range="genderOptions" range-key="label" :value="genderIndex" @change="setGender">
            <view class="field"><text class="label">性别</text><text class="value">{{ genderOptions[genderIndex].label }} ›</text></view>
          </picker>
          <picker :disabled="busy" mode="date" :value="form.birthday" @change="setBirthday">
            <view class="field last"><text class="label">生日</text><text class="value">{{ form.birthday || '选填' }} ›</text></view>
          </picker>
          <button v-if="form.birthday" class="text-button clear-date" :disabled="busy" @tap="form.birthday = ''">清除生日</button>
        </template>
      </view>

      <view v-if="!onboarding" class="preferences">
        <view class="section-title">训练偏好</view>
        <view class="form-card note-card">
          <textarea v-model="form.remark" :disabled="busy" maxlength="200" placeholder="训练目标、喜欢的动作，或希望教练留意的事" />
          <view class="character-count">{{ form.remark.length }} / 200</view>
        </view>
      </view>
      <view v-if="saveError" class="error-message">{{ saveError }}</view>
      <button class="primary" :disabled="busy || choosing" @tap="submitProfile">
        {{ saving ? '正在保存…' : onboarding ? '保存并继续' : '保存资料' }}
      </button>
    </template>
    <button v-if="onboarding" class="skip" :disabled="busy || loading || choosing" @tap="skipProfile">
      {{ skipping ? '正在继续…' : '暂时跳过' }}
    </button>
    <view v-if="loadError && saveError" class="error-message">{{ saveError }}</view>
  </view>
</template>

<script>
import MemberAvatar from '../../components/MemberAvatar.vue'
import { memberMineProfile, memberUpdateProfile } from '../../api/auth'
import { MAX_AVATAR_BYTES, uploadMemberAvatar } from '../../api/member-avatar'
import { getMemberSession, saveMemberSession, memberProfileTarget } from '../../utils/auth'

function profileForm(profile = {}) {
  return {
    nickname: profile.nickname || '',
    contactMobile: profile.contactMobile || '',
    mobile: profile.mobile || '',
    realName: profile.realName || '',
    gender: profile.gender || '0',
    birthday: String(profile.birthday || '').slice(0, 10),
    remark: profile.remark || '',
    avatarUrl: profile.avatarUrl || ''
  }
}

const genderOptions = [
  { label: '暂不填写', value: '0' }, { label: '男', value: '1' }, { label: '女', value: '2' }
]
const optionalPhone = /^(1[3-9]\d{9}|\+[1-9]\d{6,14})$/

export default {
  components: { MemberAvatar },
  data() {
    return {
      onboarding: false, target: '/pages/index/index', memberId: '', profileSetupStatus: '', active: true,
      loading: true, saving: false, skipping: false, choosing: false,
      loadError: '', saveError: '', avatarFile: '', uploadedAvatar: '',
      form: profileForm(), original: profileForm(), genderOptions
    }
  },
  computed: {
    busy() { return this.saving || this.skipping },
    genderIndex() { return Math.max(0, genderOptions.findIndex(option => option.value === this.form.gender)) }
  },
  onLoad(options = {}) {
    this.onboarding = options.onboarding === '1'
    this.target = memberProfileTarget(options.target)
    this.memberId = getMemberSession().memberInfo?.memberId
    this.loadProfile()
  },
  onUnload() { this.active = false },
  methods: {
    sameMember() {
      return this.active && this.memberId != null && getMemberSession().memberInfo?.memberId === this.memberId
    },
    async loadProfile() {
      this.loading = true
      this.loadError = ''
      try {
        const profile = await memberMineProfile()
        if (!this.sameMember()) return
        if (!profile) throw new Error('未能读取会员资料')
        this.form = profileForm(profile)
        this.original = { ...this.form }
        this.profileSetupStatus = profile.profileSetupStatus || ''
        if (this.onboarding && ['completed', 'skipped'].includes(profile.profileSetupStatus)) {
          this.updateSession({ profileSetupStatus: profile.profileSetupStatus })
          uni.reLaunch({ url: this.target })
        }
      } catch (error) {
        this.loadError = error.message || '资料读取失败，请重试'
      } finally {
        this.loading = false
      }
    },
    setGender(event) { this.form.gender = genderOptions[Number(event.detail.value)].value },
    setBirthday(event) { this.form.birthday = event.detail.value || '' },
    chooseAvatar() {
      if (this.busy || this.choosing || this.loading || this.loadError) return
      this.choosing = true
      this.saveError = ''
      uni.chooseImage({
        count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'],
        success: result => {
          if (!this.active) return
          if (result.tempFiles?.[0]?.size > MAX_AVATAR_BYTES) {
            this.saveError = '请选择 5 MB 以内的图片'
            return
          }
          if (result.tempFilePaths?.[0]) {
            this.avatarFile = result.tempFilePaths[0]
            this.uploadedAvatar = ''
          }
        },
        fail: error => {
          if (!/cancel/i.test(error?.errMsg || '')) this.saveError = '未能选择图片，请重试'
        },
        complete: () => { this.choosing = false }
      })
    },
    removeAvatar() {
      this.avatarFile = ''
      this.uploadedAvatar = ''
      this.form.avatarUrl = ''
      this.saveError = ''
    },
    updateSession(fields) {
      if (!this.sameMember()) return
      const session = getMemberSession()
      saveMemberSession({ ...session, memberInfo: { ...session.memberInfo, ...fields } })
    },
    changes() {
      const patch = {}
      const fields = this.onboarding ? ['nickname', 'contactMobile'] : ['nickname', 'contactMobile', 'realName', 'gender', 'remark']
      for (const field of fields) {
        const value = this.form[field].trim()
        if (value !== this.original[field]) patch[field] = value
      }
      if (!this.onboarding && this.form.birthday !== this.original.birthday) {
        if (this.form.birthday) patch.birthday = this.form.birthday
        else patch.clearBirthday = true
      }
      const avatarUrl = this.avatarFile ? this.uploadedAvatar : this.form.avatarUrl
      if (avatarUrl !== this.original.avatarUrl) patch.avatarUrl = avatarUrl
      return patch
    },
    async submitProfile() {
      if (this.busy || this.choosing || this.loading || this.loadError || !this.sameMember()) return
      this.saveError = ''
      const phone = this.form.contactMobile.trim()
      if (phone && !optionalPhone.test(phone)) {
        this.saveError = '请输入 11 位手机号，国际号码请以 +国家码 开头'
        return
      }
      this.saving = true
      try {
        if (this.avatarFile && !this.uploadedAvatar) this.uploadedAvatar = await uploadMemberAvatar(this.avatarFile)
        if (!this.sameMember()) return
        const patch = this.changes()
        if (this.onboarding || this.profileSetupStatus === 'pending') patch.profileSetupAction = 'complete'
        if (Object.keys(patch).length) await memberUpdateProfile(patch)
        if (!this.sameMember()) return
        if (this.avatarFile) this.form.avatarUrl = this.uploadedAvatar
        this.form.nickname = this.form.nickname.trim()
        this.form.contactMobile = phone
        this.form.realName = this.form.realName.trim()
        this.form.remark = this.form.remark.trim()
        this.original = { ...this.form }
        this.avatarFile = this.uploadedAvatar = ''
        const changed = {}
        for (const field of ['nickname', 'realName', 'contactMobile', 'avatarUrl']) {
          if (field in patch) changed[field] = patch[field]
        }
        if (patch.profileSetupAction === 'complete') {
          this.profileSetupStatus = 'completed'
          changed.profileSetupStatus = 'completed'
        }
        this.updateSession(changed)
        if (this.onboarding) uni.reLaunch({ url: this.target })
        else uni.showToast({ title: '资料已保存', icon: 'success' })
      } catch (error) {
        this.saveError = error.message || '保存失败，请重试'
      } finally {
        this.saving = false
      }
    },
    async skipProfile() {
      if (!this.onboarding || this.busy || this.choosing || this.loading || !this.sameMember()) return
      this.skipping = true
      this.saveError = ''
      try {
        await memberUpdateProfile({ profileSetupAction: 'skip' })
        if (!this.sameMember()) return
        this.updateSession({ profileSetupStatus: 'skipped' })
        uni.reLaunch({ url: this.target })
      } catch (error) {
        this.saveError = error.message || '未能跳过，请重试'
      } finally {
        this.skipping = false
      }
    }
  }
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh; box-sizing: border-box; max-width: 860rpx; margin: auto;
  padding: 40rpx 32rpx calc(56rpx + env(safe-area-inset-bottom)); background: #111211; color: #f6f4ef;
}
.page-title { font-size: 48rpx; line-height: 1.25; font-weight: 700; }
.intro, .hint, .field-note, .character-count { color: #abaea6; line-height: 1.6; }
.intro { margin-top: 14rpx; font-size: 26rpx; }
.avatar-row { display: flex; align-items: center; gap: 24rpx; padding: 40rpx 0; }
.avatar-actions { flex: 1; }
.avatar-button, .text-button, .skip { margin: 0; color: #f6f4ef; background: transparent; font-weight: 500; }
button::after { border: 0; }
.avatar-button { padding: 0; font-size: 30rpx; line-height: 1.6; text-align: left; }
.hint { display: block; margin-top: 6rpx; font-size: 22rpx; }
.text-button { padding: 12rpx 0; font-size: 26rpx; color: #ff985d; }
.remove { color: #abaea6; padding: 20rpx 4rpx; }
.form-card { border: 1rpx solid #33352f; border-radius: 20rpx; padding: 0 26rpx; background: #1d1e1b; }
.field { display: flex; align-items: center; justify-content: space-between; gap: 18rpx; min-height: 102rpx; border-bottom: 1rpx solid #32342f; }
.field.last { border-bottom: 0; }
.label { white-space: nowrap; color: #c9cbc4; font-size: 27rpx; }
.input, .value { min-width: 0; flex: 1; font-size: 28rpx; color: #f6f4ef; text-align: right; }
.input { height: 90rpx; }
.field-note { padding: 18rpx 0; font-size: 22rpx; }
.section-title { margin: 32rpx 0 16rpx; font-size: 27rpx; font-weight: 600; }
.note-card { padding: 22rpx 26rpx; }
.note-card textarea { width: 100%; height: 154rpx; font-size: 27rpx; line-height: 1.6; }
.character-count { text-align: right; font-size: 21rpx; }
.primary { margin-top: 32rpx; background: #ff7a35; color: #111211; font-size: 30rpx; border-radius: 14rpx; font-weight: 700; line-height: 88rpx; }
.skip { margin-top: 12rpx; width: 100%; font-size: 27rpx; line-height: 80rpx; }
.clear-date { margin-left: auto; text-align: right; }
.notice, .error-message { margin: 28rpx 0 0; font-size: 26rpx; }
.error-message { color: #ffb18c; }
button[disabled] { opacity: .55; }
</style>
