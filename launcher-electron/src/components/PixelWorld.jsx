export function PixelWorld() {
  return (
    <div className="pixel-world" aria-hidden="true">
      <div className="sun" />
      <div className="stars">
        {Array.from({ length: 10 }).map((_, index) => <i key={index} />)}
      </div>
      <div className="cloud one" />
      <div className="cloud two" />
      <div className="mountain one" />
      <div className="mountain two" />
      <div className="tree left"><span /></div>
      <div className="tree right"><span /></div>
      <div className="portal">
        <span />
      </div>
      <div className="campfire"><span /></div>
      <div className="ground">
        {Array.from({ length: 18 }).map((_, index) => <i key={index} />)}
      </div>
      <div className="avatar" />
    </div>
  );
}
