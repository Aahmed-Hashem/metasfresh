import React from 'react';
import ReactDOM from 'react-dom';

import App from './containers/App';

// if (process.env.NODE_ENV !== 'production') {
//   const {whyDidYouUpdate} = require('why-did-you-update')
//   // whyDidYouUpdate(React);
//   whyDidYouUpdate(React, { include: [/ListWidget/] });
// }

ReactDOM.render(<App />, document.getElementById('root'));
