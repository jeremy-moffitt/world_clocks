import { useEffect, useState } from "react";

const REFRESH_INTERVAL_MS = 30_000;

function App() {
  const [clocks, setClocks] = useState([]);
  const [receivedAt, setReceivedAt] = useState(null);
  const [now, setNow] = useState(Date.now());
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    async function loadClocks() {
      try {
        const response = await fetch("/api/clocks", { signal: controller.signal });
        if (!response.ok) {
          throw new Error(`The clock server returned HTTP ${response.status}.`);
        }

        const data = await response.json();
        if (!Array.isArray(data.clocks) || data.clocks.length !== 8) {
          throw new Error("The clock server did not return eight cities.");
        }

        const invalidClock = data.clocks.some(
          (clock) =>
            !clock.city ||
            !clock.country ||
            !clock.timeZone ||
            !Number.isFinite(Date.parse(clock.currentTime)),
        );
        if (invalidClock) {
          throw new Error("The clock server returned incomplete clock data.");
        }

        if (active) {
          setClocks(data.clocks);
          setReceivedAt(Date.now());
          setError("");
        }
      } catch (loadError) {
        if (active && loadError.name !== "AbortError") {
          setError(loadError.message || "Unable to load clocks.");
        }
      }
    }

    loadClocks();
    const refreshTimer = window.setInterval(loadClocks, REFRESH_INTERVAL_MS);
    const tickTimer = window.setInterval(() => setNow(Date.now()), 1000);

    return () => {
      active = false;
      controller.abort();
      window.clearInterval(refreshTimer);
      window.clearInterval(tickTimer);
    };
  }, []);

  return (
    <main className="page-shell">
      <header className="page-header">
        <div className="eyebrow">A little perspective, everywhere</div>
        <h1>World clocks</h1>
        <p>Eight cities, picked at random. Time moves on; this page keeps up.</p>
      </header>

      {error && (
        <div className="status-message" role="status">
          {clocks.length === 0
            ? `${error} Start the Java server and this page will try again automatically.`
            : `${error} Showing the last available times.`}
        </div>
      )}

      {clocks.length === 0 && !error && (
        <div className="status-message" role="status">
          Loading clocks…
        </div>
      )}

      {clocks.length > 0 && (
        <section className="clock-grid" aria-label="Current city times">
          {clocks.map((clock) => {
            const cityTime =
              Date.parse(clock.currentTime) + (now - (receivedAt ?? now));
            const time = new Intl.DateTimeFormat(undefined, {
              timeZone: clock.timeZone,
              hour: "2-digit",
              minute: "2-digit",
              second: "2-digit",
              hourCycle: "h23",
            }).format(cityTime);
            const date = new Intl.DateTimeFormat(undefined, {
              timeZone: clock.timeZone,
              weekday: "long",
              month: "long",
              day: "numeric",
              year: "numeric",
            }).format(cityTime);

            return (
              <article className="clock-card" key={clock.timeZone}>
                <div className="clock-card-top">
                  <span className="clock-dot" aria-hidden="true" />
                  <span className="timezone">{clock.timeZone.replaceAll("_", " ")}</span>
                </div>
                <h2>{clock.city}</h2>
                <p className="country">{clock.country}</p>
                <time className="clock-time" dateTime={new Date(cityTime).toISOString()}>
                  {time}
                </time>
                <p className="clock-date">{date}</p>
              </article>
            );
          })}
        </section>
      )}

      <footer>
        Locations are reshuffled by the server every five minutes.
      </footer>
    </main>
  );
}

export default App;
