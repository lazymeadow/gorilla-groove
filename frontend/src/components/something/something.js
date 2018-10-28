import React from 'react';

export class Something extends React.Component {
  render() {
    let clazz = "best-style";
    return <div className={clazz}>Yo dawg</div>;
  }
}