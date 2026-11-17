import { useEffect, useState } from "react";

const CURRENT_VERSION = "2.0.0";
const RELEASE_API = "https://api.github.com/repos/Jigar1234Nob/JVC/releases/latest";
const RELEASES_PAGE = "https://github.com/Jigar1234Nob/JVC/releases";
const CHAT_URL = "https://chat.jigar.au";

type ReleaseAsset = { name: string; browser_download_url: string };
type Release = { tag_name: string; html_url: string; assets: ReleaseAsset[] };

function versionParts(value: string) {
  return value.replace(/^v/i, "").split(".").map(part => Number.parseInt(part, 10) || 0);
}

function isNewer(latest: string, current: string) {
  const a = versionParts(latest);
  const b = versionParts(current);
  for (let i = 0; i < Math.max(a.length, b.length); i++) {
    if ((a[i] || 0) !== (b[i] || 0)) return (a[i] || 0) > (b[i] || 0);
  }
  return false;
}

function getPlatform() {
  const ua = navigator.userAgent;
  if (/Android/i.test(ua)) return "android";
  if (/Macintosh|Mac OS X/i.test(ua)) return "macos";
  if (/Windows/i.test(ua)) return "windows";
  return "other";
}

function matchingAsset(assets: ReleaseAsset[], platform: string) {
  const names = assets.map(asset => ({ asset, name: asset.name.toLowerCase() }));
  if (platform === "android") return names.find(x => x.name.endsWith(".apk"))?.asset;
  if (platform === "windows") {
    return names.find(x => x.name.endsWith(".exe"))?.asset
      || names.find(x => x.name.endsWith(".msi"))?.asset;
  }
  if (platform === "macos") return names.find(x => x.name.endsWith(".dmg"))?.asset;
  return undefined;
}

export default function App() {
  const [status, setStatus] = useState("Checking for updates…");
  const [release, setRelease] = useState<Release | null>(null);
  const [downloadUrl, setDownloadUrl] = useState("");
  const [platform] = useState(getPlatform);
  const [error, setError] = useState(false);

  useEffect(() => {
    let active = true;
    let redirectTimer: number | undefined;
    const controller = new AbortController();
    const checkTimer = window.setTimeout(() => controller.abort(), 8000);

    const check = async () => {
      try {
        const response = await fetch(RELEASE_API, {
          headers: { Accept: "application/vnd.github+json" },
          cache: "no-store",
          signal: controller.signal
        });
        if (!response.ok) throw new Error(`GitHub returned ${response.status}`);
        const latest = await response.json() as Release;
        if (!active) return;

        if (latest.tag_name && isNewer(latest.tag_name, CURRENT_VERSION)) {
          setRelease(latest);
          const asset = matchingAsset(latest.assets || [], platform);
          setDownloadUrl(asset?.browser_download_url || latest.html_url || RELEASES_PAGE);
          setStatus("An update is available");
          return;
        }

        window.location.replace(CHAT_URL);
      } catch {
        if (!active) return;
        setError(true);
        setStatus("Couldn’t check for updates");
        // A GitHub outage must never prevent the user from opening JVC.
        redirectTimer = window.setTimeout(() => window.location.replace(CHAT_URL), 1800);
      } finally {
        window.clearTimeout(checkTimer);
      }
    };

    void check();
    return () => {
      active = false;
      controller.abort();
      window.clearTimeout(checkTimer);
      if (redirectTimer !== undefined) window.clearTimeout(redirectTimer);
    };
  }, [platform]);

  const continueToJvc = () => window.location.replace(CHAT_URL);
  const openDownload = () => {
    let target = RELEASES_PAGE;
    try {
      const candidate = new URL(downloadUrl || RELEASES_PAGE);
      if (candidate.protocol === "https:" && candidate.hostname === "github.com") {
        target = candidate.href;
      }
    } catch {
      target = RELEASES_PAGE;
    }
    window.open(target, "_blank", "noopener,noreferrer");
  };

  return (
    <main className="startup">
      <section className="startup-card">
        <div className="brand-mark">
          <img src="/jvc-logo.svg" alt="JVC logo" />
        </div>
        <p className="eyebrow">JIGAR VIRTUAL CLIENT</p>
        <h1>JVC</h1>
        <p className="version">Version {CURRENT_VERSION}</p>
        <div className={error ? "status status-error" : "status"}>
          {!error && !release && <span className="spinner" />}
          {status}
        </div>
        {release && (
          <div className="update-panel">
            <p className="update-version">Version {release.tag_name.replace(/^v/i, "")} is ready</p>
            <p className="update-copy">
              {platform === "android"
                ? "There’s a newer Android version available. Install it to get the latest fixes and improvements."
                : "A newer version of JVC is available for your device."}
            </p>
            <button className="primary-button" onClick={openDownload}>
              Download update
            </button>
            {platform === "android" && (
              <button className="text-button" onClick={continueToJvc}>
                Skip for now
              </button>
            )}
            {platform !== "android" && (
              <button className="text-button" onClick={continueToJvc}>
                Continue with this version
              </button>
            )}
          </div>
        )}
        <p className="footer-note">Think Beyond. Build Beyond.</p>
      </section>
    </main>
  );
}
