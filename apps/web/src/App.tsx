const CHAT_URL = "https://chat.jigar.au";

export default function App() {
  return (
    <main className="site">
      <iframe
        title="JVC"
        src={CHAT_URL}
        allow="camera; microphone; clipboard-read; clipboard-write; display-capture; fullscreen; autoplay; notifications"
        allowFullScreen
        referrerPolicy="strict-origin-when-cross-origin"
      />
    </main>
  );
}
