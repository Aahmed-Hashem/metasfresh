import counterpart from 'counterpart';
import PropTypes from 'prop-types';
import React, { Component } from 'react';
import onClickOutside from 'react-onclickoutside';
import { connect } from 'react-redux';
import { push } from 'react-router-redux';

import {
  deleteUserNotification,
  markAllAsRead,
  markAsRead,
} from '../../actions/AppActions';
import InboxItem from './InboxItem';

class Inbox extends Component {
  constructor(props) {
    super(props);
  }

  handleClickOutside = () => {
    const { close } = this.props;

    close && close();
  };

  handleClick = item => {
    const { dispatch, close } = this.props;
    if (item.target) {
      switch (item.target.targetType) {
        case 'window':
          dispatch(
            push(`/window/${item.target.windowId}/${item.target.documentId}`)
          );
          break;
        case 'view':
          dispatch(
            push(
              `/window/${item.target.windowId}/?viewId=${item.target.viewId}`
            )
          );
          break;
      }
    }

    if (!item.read) {
      markAsRead(item.id);
    }
    close && close();
  };

  handleMarkAllAsRead = () => {
    const { close } = this.props;

    markAllAsRead();

    close && close();
  };

  handleShowAll = () => {
    const { close, dispatch } = this.props;
    dispatch(push('/inbox'));
    close && close();
  };

  handleDelete = (e, item) => {
    e.preventDefault();
    e.stopPropagation();

    deleteUserNotification(item.id).then(() => {});
  };

  handleKeyDown = e => {
    const { close } = this.props;
    const inboxItem = document.getElementsByClassName('js-inbox-item')[0];
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        if (document.activeElement.classList.contains('js-inbox-wrapper')) {
          if (inboxItem) {
            inboxItem.focus();
          }
        }
        break;

      case 'Tab':
        close && close();
        break;

      case 'Escape':
        e.preventDefault();
        close && close();
        break;
    }
  };

  render() {
    const { open, inbox, all, close, modalVisible, onFocus } = this.props;

    return (
      <div
        className="js-inbox-wrapper"
        onKeyDown={e => this.handleKeyDown(e)}
        onFocus={onFocus}
        tabIndex={modalVisible ? -1 : 0}
      >
        {(all || open) && (
          <div className={all ? 'inbox-all ' : 'inbox'}>
            <div className={'inbox-body ' + (!all ? 'breadcrumbs-shadow' : '')}>
              <div className="inbox-header">
                <span className="inbox-header-title">
                  {counterpart.translate('window.inbox.caption')}
                </span>
                <span onClick={this.handleMarkAllAsRead} className="inbox-link">
                  {counterpart.translate('window.markAsRead.caption')}
                </span>
              </div>
              <div className={!all ? 'inbox-list' : ''}>
                {inbox &&
                  inbox.notifications.map((item, id) => (
                    <InboxItem
                      key={id}
                      item={item}
                      close={close}
                      onClick={() => this.handleClick(item)}
                      onDelete={e => this.handleDelete(e, item)}
                    />
                  ))}
                {inbox &&
                  inbox.notifications.length == 0 && (
                    <div className="inbox-item inbox-item-empty">
                      {counterpart.translate('window.inbox.empty')}
                    </div>
                  )}
              </div>
              <div className="inbox-footer">
                {!all && (
                  <div
                    onClick={this.handleShowAll}
                    className="inbox-link text-xs-center"
                  >
                    {counterpart.translate('window.allInbox.caption')} &gt;&gt;
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }
}

Inbox.propTypes = {
  dispatch: PropTypes.func.isRequired,
  open: PropTypes.bool,
  onFocus: PropTypes.func,
  modalVisible: PropTypes.bool.isRequired,
};

Inbox.defaultProps = {
  onFocus: () => {},
};

export default connect(state => ({
  modalVisible: state.windowHandler.modal.visible,
}))(onClickOutside(Inbox));
