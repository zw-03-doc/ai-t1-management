<template>
  <main class="venue-manager" v-loading="busy">
    <header>
      <div class="venue-heading">
        <h1>场馆图片</h1>
        <el-select v-if="stores.length > 1" :model-value="selectedId" aria-label="门店" placeholder="选择门店" filterable
          :disabled="saving" @change="selectStore">
          <el-option v-for="store in stores" :key="store.storeId" :value="String(store.storeId)"
            :label="store.storeName + (store.status === '0' ? '' : '（已停用）')" />
        </el-select>
        <span v-else-if="stores.length">{{ stores[0].storeName }}</span>
      </div>
      <div>
        <el-button :disabled="busy" @click="loadStores">刷新</el-button>
        <el-button v-if="canEdit" type="primary" :disabled="busy || !gallery" @click="choose()">上传图片</el-button>
      </div>
    </header>
    <input ref="picker" type="file" accept="image/jpeg,image/png" hidden @change="onFile" />
    <el-alert v-if="error" :title="error" type="warning" :closable="false" />
    <div v-if="gallery?.photos.length" class="photos">
      <article v-for="(photo, index) in gallery.photos" :key="photo.id">
        <el-image :src="imageSrc(photo)" fit="cover" :preview-src-list="gallery.photos.map(imageSrc)" :initial-index="index" preview-teleported />
        <footer v-if="canEdit">
          <div>
            <el-button link :disabled="busy || index === 0" @click="move(index, -1)">前移</el-button>
            <el-button link :disabled="busy || index === gallery.photos.length - 1" @click="move(index, 1)">后移</el-button>
          </div>
          <div>
            <el-button link :disabled="busy" @click="choose(photo.id)">更换</el-button>
            <el-button link type="danger" :disabled="busy" @click="remove(photo)">删除</el-button>
          </div>
        </footer>
      </article>
    </div>
    <el-empty v-else-if="gallery" description="这家门店暂无场馆照片" />
    <el-empty v-else-if="!busy && !error && !stores.length" description="暂无门店" />
  </main>
</template>

<script lang="ts" setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { checkPermi } from '@/utils/permission';
import { deletePhoto, getGallery, getPrivateImage, getVenueStores, reorderPhotos, uploadPhoto, type VenueGallery, type VenuePhoto, type VenueStore } from '@/api/fitness/venue';

const canEdit = computed(() => checkPermi(['fitness:venue:edit']));
const route = useRoute();
const router = useRouter();
const stores = ref<VenueStore[]>([]);
const selectedId = ref('');
const gallery = ref<VenueGallery>();
const busy = ref(false);
const saving = ref(false);
const error = ref('');
const picker = ref<HTMLInputElement>();
const privateImages = ref<Record<string, string>>({});
let request = 0;
let fileTarget: { store: string; version: number; replace?: string } | undefined;

function releaseImages() {
  Object.values(privateImages.value).forEach(url => URL.revokeObjectURL(url));
  privateImages.value = {};
}
function imageSrc(photo: VenuePhoto) {
  if (photo.url.startsWith('/app/static/images/')) return photo.url;
  return privateImages.value[photo.id] || import.meta.env.VITE_APP_BASE_API + photo.url;
}
async function showAlbum(album: VenueGallery, number: number) {
  if (number !== request || String(album.storeId) !== selectedId.value) return;
  const hidden = stores.value.find(store => String(store.storeId) === selectedId.value)?.status !== '0';
  const images = hidden ? await Promise.all(album.photos.filter(photo => !photo.url.startsWith('/app/static/'))
    .map(async photo => ({ id: photo.id, blob: await getPrivateImage(String(album.storeId), photo.id) }))) : [];
  if (number !== request) return;
  releaseImages();
  for (const image of images) privateImages.value[image.id] = URL.createObjectURL(image.blob);
  gallery.value = album;
}

function selectStore(id: string) {
  void router.replace({ query: { ...route.query, storeId: id } });
}
async function loadStores() {
  const number = ++request;
  busy.value = true;
  saving.value = false;
  error.value = '';
  gallery.value = undefined;
  releaseImages();
  try {
    const { data } = await getVenueStores();
    if (number !== request) return;
    stores.value = data;
    selectedId.value = String(route.query.storeId ?? data[0]?.storeId ?? '');
    if (!selectedId.value) return;
    if (!data.some(store => String(store.storeId) === selectedId.value)) {
      throw new Error('找不到这家门店');
    }
    await showAlbum((await getGallery(selectedId.value)).data, number);
  } catch (err) {
    if (number === request) error.value = err instanceof Error ? err.message : '场馆图片读取失败，请刷新。';
  } finally { if (number === request) busy.value = false; }
}

async function save(store: string, version: number, action: () => Promise<{ data: VenueGallery }>) {
  if (!gallery.value || busy.value || !canEdit.value || store !== selectedId.value || version !== gallery.value.version) return;
  const number = ++request;
  saving.value = busy.value = true;
  try {
    await showAlbum((await action()).data, number);
    if (number !== request) return;
    error.value = '';
    ElMessage.success('已保存');
  } catch {
    if (number === request) error.value = '未确认保存结果，请刷新后重试。';
  } finally { if (number === request) saving.value = busy.value = false; }
}

function choose(id?: string) {
  if (!gallery.value) return;
  fileTarget = { store: selectedId.value, version: gallery.value.version, replace: id };
  picker.value?.click();
}
function onFile(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  const target = fileTarget;
  fileTarget = undefined;
  input.value = '';
  if (!file || !target) return;
  if (file.size > 10 * 1024 * 1024 || !['image/jpeg', 'image/png'].includes(file.type)) {
    ElMessage.warning('请选择 10 MB 以内的 JPEG 或 PNG 图片');
    return;
  }
  void save(target.store, target.version, () => uploadPhoto(target.store, target.version, file, target.replace));
}
function move(index: number, delta: number) {
  const album = gallery.value;
  if (!album) return;
  const ids = album.photos.map(photo => photo.id);
  const to = index + delta;
  if (to < 0 || to >= ids.length) return;
  [ids[index], ids[to]] = [ids[to], ids[index]];
  void save(album.storeId, album.version, () => reorderPhotos(album.storeId, album.version, ids));
}
async function remove(photo: VenuePhoto) {
  const album = gallery.value;
  if (!album) return;
  try {
    await ElMessageBox.confirm('删除这张场馆照片？', '删除场馆照片', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' });
  } catch { return; }
  await save(album.storeId, album.version, () => deletePhoto(album.storeId, album.version, photo.id));
}
watch(() => route.query.storeId, loadStores);
onMounted(loadStores);
onBeforeUnmount(() => { request++; releaseImages(); });
</script>

<style scoped>
.venue-manager { max-width: 1160px; margin: 0 auto; padding: clamp(16px, 4vw, 40px); }
header, footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
header { flex-wrap: wrap; margin-bottom: 24px; }
.venue-heading { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; min-width: 0; }
.venue-heading .el-select { width: min(280px, 78vw); }
.venue-heading span { color: var(--el-text-color-secondary); overflow-wrap: anywhere; }
h1 { font-size: 26px; margin: 0; }
.photos { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 245px), 1fr)); gap: 20px; }
article { overflow: hidden; border: 1px solid var(--el-border-color-light); border-radius: 12px; background: var(--el-bg-color); }
.el-image { display: block; width: 100%; aspect-ratio: 4 / 3; cursor: zoom-in; }
footer { padding: 10px 12px; }
</style>
