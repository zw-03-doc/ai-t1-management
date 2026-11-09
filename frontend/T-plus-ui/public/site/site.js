async function showVenue() {
  const response = await fetch('/prod-api/fitness/mini/portal/venue', { cache: 'no-store' });
  if (!response.ok) return;
  const result = await response.json();
  const path = result.data?.photos?.[0]?.url;
  if (result.code !== 200 || typeof path !== 'string') return;
  const bundled = /^\/app\/static\/images\/live-[123]\.jpg$/.test(path);
  if (!bundled && !/^\/fitness\/mini\/portal\/venue\/images\/[\w-]+$/.test(path)) return;
  const image = document.querySelector('#venue-photo');
  image.onload = () => { image.parentElement.hidden = false; };
  image.src = bundled ? path : '/prod-api' + path;
}
// Preserve the workbench bookmark for administrators opening the domain root.
function openWorkbench() {
  try {
    if (location.pathname === '/' && localStorage.getItem('Admin-Token')) {
      location.replace('/index');
      return true;
    }
  } catch { /* The public site also works with storage disabled. */ }
  return false;
}
if (!openWorkbench()) showVenue().catch(() => { /* All navigation stays available offline. */ });
