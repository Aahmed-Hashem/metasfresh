import { Component } from 'react';
import PropTypes from 'prop-types';
import keymap from './keymap';

export default class Shortcut extends Component {
    static contextTypes = {
        shortcuts: PropTypes.shape({
            subscribe: PropTypes.func.isRequired,
            unsubscribe: PropTypes.func.isRequired
        }).isRequired
    };

    static propTypes = {
        name: PropTypes.oneOf(Object.keys(keymap)).isRequired,
        handler: PropTypes.func.isRequired
    };

    componentWillMount() {
        const { subscribe } = this.context.shortcuts;
        const { name, handler } = this.props;

        subscribe(name, handler);
    }

    componentWillUnmount() {
        const { unsubscribe } = this.context.shortcuts;
        const { name, handler } = this.props;

        unsubscribe(name, handler);
    }

    render() {
        return null;
    }
}
