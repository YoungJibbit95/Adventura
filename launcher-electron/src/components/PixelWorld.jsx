export function PixelWorld() {
  return (
    <div className="pixel-world" aria-hidden="true">
      <div className="moon" />
      <div className="stars">
        {Array.from({ length: 10 }).map((_, index) => <i key={index} />)}
      </div>
      <div className="mountain one" />
      <div className="mountain two" />
      <div className="portal">
        <span />
      </div>
      <div className="ground">
        {Array.from({ length: 18 }).map((_, index) => <i key={index} />)}
      </div>
      <div className="avatar" />
    </div>
  );
}
