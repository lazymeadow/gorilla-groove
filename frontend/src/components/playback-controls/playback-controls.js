import React from 'react';

export class PlaybackControls extends React.Component {
  render() {
    return <div>
        --- Progress Bar ---
        <div>
            <button>Start</button>
            <button>Stop</button>
        </div>
    </div>;
  }
}