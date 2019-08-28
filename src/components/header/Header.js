import counterpart from 'counterpart';
import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { push } from 'react-router-redux';
import classnames from 'classnames';

import {
  deleteRequest,
  duplicateRequest,
  openFile,
} from '../../actions/GenericActions';
import { openModal } from '../../actions/WindowActions';
import logo from '../../assets/images/metasfresh_logo_green_thumb.png';
import keymap from '../../shortcuts/keymap';
import Indicator from '../app/Indicator';
import Prompt from '../app/Prompt';
import NewEmail from '../email/NewEmail';
import Inbox from '../inbox/Inbox';
import NewLetter from '../letter/NewLetter';
import GlobalContextShortcuts from '../keyshortcuts/GlobalContextShortcuts';
import Tooltips from '../tooltips/Tooltips';
import MasterWidget from '../widget/MasterWidget';
import Breadcrumb from './Breadcrumb';
import SideList from './SideList';
import Subheader from './SubHeader';
import UserDropdown from './UserDropdown';

/**
 * @file The Header component is shown in every view besides Modal or RawModal in frontend. It defines
 * the top bar with different menus and icons in metasfresh WebUI. It hosts the action menu,
 * breadcrumb, logo, notification menu, avatar and sidelist menu.
 * @module Header
 * @extends Component
 */
class Header extends Component {
  state = {
    isSubheaderShow: false,
    isSideListShow: false,
    sideListTab: null,
    isMenuOverlayShow: false,
    menuOverlay: null,
    scrolled: false,
    isInboxOpen: false,
    isUDOpen: false,
    tooltipOpen: '',
    isEmailOpen: false,
    prompt: { open: false },
  };

  udRef = React.createRef();
  inboxRef = React.createRef();

  componentDidMount() {
    this.initEventListeners();
  }

  componentWillUnmount() {
    this.toggleScrollScope(false);
    this.removeEventListeners();
  }

  UNSAFE_componentWillUpdate(nextProps) {
    const { dropzoneFocused } = this.props;

    if (
      nextProps.dropzoneFocused !== dropzoneFocused &&
      nextProps.dropzoneFocused
    ) {
      this.closeOverlays();
    }
  }

  componentDidUpdate(prevProps, prevState) {
    // const {dispatch, pathname} = this.props;

    if (
      prevProps.me.language !== undefined &&
      JSON.stringify(prevProps.me.language) !==
        JSON.stringify(this.props.me.language)
    ) {
      /*
            dispatch(replace(''));
            dispatch(replace(pathname));
            */

      // Need to reload page completely when current locale gets changed
      window.location.reload(false);
    } else if (
      this.state.isUDOpen &&
      !prevState.isUDOpen &&
      !!this.udRef.current
    ) {
      this.udRef.current.enableOnClickOutside();
    } else if (
      !this.state.isUDOpen &&
      prevState.isUDOpen &&
      !!this.udRef.current
    ) {
      this.udRef.current.disableOnClickOutside();
    } else if (
      this.state.isInboxOpen &&
      !prevState.isInboxOpen &&
      !!this.inboxRef.current
    ) {
      this.inboxRef.current.enableOnClickOutside();
    } else if (
      !this.state.isInboxOpen &&
      prevState.isInboxOpen &&
      !!this.inboxRef.current
    ) {
      this.inboxRef.current.disableOnClickOutside();
    }
  }

  initEventListeners = () => {
    document.addEventListener('scroll', this.handleScroll);
  };

  removeEventListeners = () => {
    document.removeEventListener('scroll', this.handleScroll);
  };

  handleInboxOpen = state => {
    this.setState({ isInboxOpen: !!state });
  };

  handleInboxToggle = () => {
    this.setState({ isInboxOpen: !this.state.isInboxOpen });
  };

  handleUDOpen = state => {
    this.setState({ isUDOpen: !!state });
  };

  handleUDToggle = () => {
    this.setState({ isUDOpen: !this.state.isUDOpen });
  };

  handleMenuOverlay = (e, nodeId) => {
    const { isSubheaderShow, isSideListShow } = this.state;

    if (e) {
      e.preventDefault();
    }

    let toggleBreadcrumb = () => {
      this.setState(
        {
          menuOverlay: nodeId,
        },
        () => {
          if (nodeId !== '') {
            this.setState({ isMenuOverlayShow: true });
          } else {
            this.setState({ isMenuOverlayShow: false });
          }
        }
      );
    };

    if (!isSubheaderShow && !isSideListShow) {
      toggleBreadcrumb();
    }
  };

  handleScroll = event => {
    const target = event.srcElement;
    let scrollTop = target && target.body.scrollTop;

    if (!scrollTop) {
      scrollTop = document.documentElement.scrollTop;
    }

    if (scrollTop > 0) {
      this.setState({ scrolled: true });
    } else {
      this.setState({ scrolled: false });
    }
  };

  handleDashboardLink = () => {
    const { dispatch } = this.props;
    dispatch(push('/'));
  };

  toggleScrollScope = open => {
    if (!open) {
      document.body.style.overflow = 'auto';
    } else {
      document.body.style.overflow = 'hidden';
    }
  };

  toggleTooltip = tooltip => {
    this.setState({ tooltipOpen: tooltip });
  };

  openModal = (
    windowId,
    modalType,
    caption,
    isAdvanced,
    selected,
    childViewId,
    childViewSelectedIds,
    staticModalType
  ) => {
    const { dispatch, query } = this.props;

    dispatch(
      openModal(
        caption,
        windowId,
        modalType,
        null,
        null,
        isAdvanced,
        query && query.viewId,
        selected,
        null,
        null,
        null,
        null,
        childViewId,
        childViewSelectedIds,
        staticModalType
      )
    );
  };

  openModalRow = (
    windowId,
    modalType,
    caption,
    tabId,
    rowId,
    staticModalType
  ) => {
    const { dispatch } = this.props;

    dispatch(
      openModal(
        caption,
        windowId,
        modalType,
        tabId,
        rowId,
        false,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        staticModalType
      )
    );
  };

  handlePrint = (windowId, docId, docNo) => {
    openFile(
      'window',
      windowId,
      docId,
      'print',
      `${windowId}_${docNo ? `${docNo}` : `${docId}`}.pdf`
    );
  };

  handleClone = (windowId, docId) => {
    const { dispatch } = this.props;

    duplicateRequest('window', windowId, docId).then(response => {
      if (response && response.data && response.data.id) {
        dispatch(push(`/window/${windowId}/${response.data.id}`));
      }
    });
  };

  handleDelete = () => {
    this.setState({
      prompt: Object.assign({}, this.state.prompt, { open: true }),
    });
  };

  handleEmail = () => {
    this.setState({ isEmailOpen: true });
  };

  handleLetter = () => {
    this.setState({ isLetterOpen: true });
  };

  handleCloseEmail = () => {
    this.setState({ isEmailOpen: false });
  };

  handleCloseLetter = () => {
    this.setState({ isLetterOpen: false });
  };

  handlePromptCancelClick = () => {
    this.setState({
      prompt: Object.assign({}, this.state.prompt, { open: false }),
    });
  };

  handlePromptSubmitClick = (windowId, docId) => {
    const { dispatch, handleDeletedStatus } = this.props;

    this.setState(
      {
        prompt: Object.assign({}, this.state.prompt, { open: false }),
      },
      () => {
        deleteRequest('window', windowId, null, null, [docId]).then(() => {
          handleDeletedStatus(true);
          dispatch(push(`/window/${windowId}`));
        });
      }
    );
  };

  handleDocStatusToggle = close => {
    const elem = document.getElementsByClassName('js-dropdown-toggler')[0];

    if (close) {
      elem.blur();
    } else {
      if (document.activeElement === elem) {
        elem.blur();
      } else {
        elem.focus();
      }
    }
  };

  handleSidelistToggle = (id = null) => {
    const { sideListTab } = this.state;

    const isSideListShow = id !== null && id !== sideListTab;

    this.toggleScrollScope(isSideListShow);

    this.setState({
      isSideListShow,
      sideListTab: id !== sideListTab ? id : null,
    });
  };

  closeOverlays = (clickedItem, callback) => {
    const { isSubheaderShow } = this.state;

    const state = {
      menuOverlay: null,
      isMenuOverlayShow: false,
      isInboxOpen: false,
      isUDOpen: false,
      isSideListShow: false,
      tooltipOpen: '',
    };

    if (clickedItem) {
      delete state[clickedItem];
    }

    state.isSubheaderShow =
      clickedItem === 'isSubheaderShow' ? !isSubheaderShow : false;

    this.setState(state, callback);

    if (
      document.getElementsByClassName('js-dropdown-toggler')[0] &&
      clickedItem !== 'dropdown'
    ) {
      this.handleDocStatusToggle(true);
    }
  };

  redirect = where => {
    const { dispatch } = this.props;
    dispatch(push(where));
  };

  render() {
    const {
      docSummaryData,
      siteName,
      docNoData,
      docStatus,
      docStatusData,
      dataId,
      breadcrumb,
      showSidelist,
      inbox,
      entity,
      query,
      showIndicator,
      windowId,
      // TODO: We should be using indicator from the state instead of another variable
      isDocumentNotSaved,
      notfound,
      docId,
      me,
      editmode,
      handleEditModeToggle,
      activeTab,
      plugins,
    } = this.props;

    const {
      isSubheaderShow,
      isSideListShow,
      menuOverlay,
      isInboxOpen,
      scrolled,
      isMenuOverlayShow,
      tooltipOpen,
      prompt,
      sideListTab,
      isUDOpen,
      isEmailOpen,
      isLetterOpen,
    } = this.state;

    return (
      <div>
        {prompt.open && (
          <Prompt
            title="Delete"
            text="Are you sure?"
            buttons={{ submit: 'Delete', cancel: 'Cancel' }}
            onCancelClick={this.handlePromptCancelClick}
            onSubmitClick={() => this.handlePromptSubmitClick(windowId, dataId)}
          />
        )}

        <nav
          className={classnames('header header-super-faded', {
            'header-shadow': scrolled,
          })}
        >
          <div className="container-fluid">
            <div className="header-container">
              <div className="header-left-side">
                <div
                  onClick={() => this.closeOverlays('isSubheaderShow')}
                  onMouseEnter={() =>
                    this.toggleTooltip(keymap.OPEN_ACTIONS_MENU)
                  }
                  onMouseLeave={() => this.toggleTooltip('')}
                  className={classnames(
                    'btn-square btn-header',
                    'tooltip-parent js-not-unselect',
                    {
                      'btn-meta-default-dark btn-subheader-open btn-header-open': isSubheaderShow,
                      'btn-meta-primary': !isSubheaderShow,
                    }
                  )}
                >
                  <i className="meta-icon-more" />

                  {tooltipOpen === keymap.OPEN_ACTIONS_MENU && (
                    <Tooltips
                      name={keymap.OPEN_ACTIONS_MENU}
                      action={counterpart.translate(
                        'mainScreen.actionMenu.tooltip'
                      )}
                      type=""
                    />
                  )}
                </div>

                <Breadcrumb
                  breadcrumb={breadcrumb}
                  windowType={windowId}
                  docSummaryData={docSummaryData}
                  dataId={dataId}
                  siteName={siteName}
                  menuOverlay={menuOverlay}
                  docId={docId}
                  isDocumentNotSaved={isDocumentNotSaved}
                  handleMenuOverlay={this.handleMenuOverlay}
                  openModal={this.openModal}
                />
              </div>
              <div className="header-center js-not-unselect">
                <img
                  src={logo}
                  className="header-logo pointer"
                  onClick={this.handleDashboardLink}
                />
              </div>
              <div className="header-right-side">
                {docStatus && (
                  <div
                    className="hidden-sm-down tooltip-parent js-not-unselect"
                    onClick={() => this.toggleTooltip('')}
                    onMouseEnter={() => this.toggleTooltip(keymap.DOC_STATUS)}
                  >
                    <MasterWidget
                      entity="window"
                      windowType={windowId}
                      dataId={dataId}
                      widgetData={[docStatusData]}
                      noLabel
                      type="primary"
                      dropdownOpenCallback={() =>
                        this.closeOverlays('dropdown')
                      }
                      {...docStatus}
                    />
                    {tooltipOpen === keymap.DOC_STATUS && (
                      <Tooltips
                        name={keymap.DOC_STATUS}
                        action={counterpart.translate(
                          'mainScreen.docStatus.tooltip'
                        )}
                        type=""
                      />
                    )}
                  </div>
                )}

                <div
                  className={classnames(
                    'header-item-container',
                    'header-item-container-static',
                    'pointer tooltip-parent js-not-unselect',
                    {
                      'header-item-open': isInboxOpen,
                    }
                  )}
                  onClick={() =>
                    this.closeOverlays('', () => this.handleInboxOpen(true))
                  }
                  onMouseEnter={() =>
                    this.toggleTooltip(keymap.OPEN_INBOX_MENU)
                  }
                  onMouseLeave={() => this.toggleTooltip('')}
                >
                  <span className="header-item header-item-badge icon-lg">
                    <i className="meta-icon-notifications" />
                    {inbox.unreadCount > 0 && (
                      <span className="notification-number">
                        {inbox.unreadCount}
                      </span>
                    )}
                  </span>
                  {tooltipOpen === keymap.OPEN_INBOX_MENU && (
                    <Tooltips
                      name={keymap.OPEN_INBOX_MENU}
                      action={counterpart.translate('mainScreen.inbox.tooltip')}
                      type={''}
                    />
                  )}
                </div>

                <Inbox
                  ref={this.inboxRef}
                  open={isInboxOpen}
                  close={this.handleInboxOpen}
                  onFocus={() => this.handleInboxOpen(true)}
                  disableOnClickOutside={true}
                  inbox={inbox}
                />

                <UserDropdown
                  ref={this.udRef}
                  open={isUDOpen}
                  handleUDOpen={this.handleUDOpen}
                  disableOnClickOutside={true}
                  redirect={this.redirect}
                  shortcut={keymap.OPEN_AVATAR_MENU}
                  toggleTooltip={this.toggleTooltip}
                  tooltipOpen={tooltipOpen}
                  me={me}
                  plugins={plugins}
                />

                {showSidelist && (
                  <div
                    className={classnames(
                      'tooltip-parent btn-header',
                      'side-panel-toggle btn-square',
                      'js-not-unselect',
                      {
                        'btn-meta-default-bright btn-header-open': isSideListShow,
                        'btn-meta-primary': !isSideListShow,
                      }
                    )}
                    onClick={() => this.handleSidelistToggle(0)}
                    onMouseEnter={() =>
                      this.toggleTooltip(keymap.OPEN_SIDEBAR_MENU_0)
                    }
                    onMouseLeave={() => this.toggleTooltip('')}
                  >
                    <i className="meta-icon-list" />
                    {tooltipOpen === keymap.OPEN_SIDEBAR_MENU_0 && (
                      <Tooltips
                        name={keymap.OPEN_SIDEBAR_MENU_0}
                        action={counterpart.translate(
                          /* eslint-disable max-len */
                          'mainScreen.sideList.tooltip'
                          /* eslint-enable max-len */
                        )}
                        type=""
                      />
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>

          {showIndicator && (
            <Indicator isDocumentNotSaved={isDocumentNotSaved} />
          )}
        </nav>

        {isSubheaderShow && (
          <Subheader
            closeSubheader={() => this.closeOverlays('isSubheaderShow')}
            docNo={docNoData && docNoData.value}
            openModal={this.openModal}
            openModalRow={this.openModalRow}
            handlePrint={this.handlePrint}
            handleClone={this.handleClone}
            handleDelete={this.handleDelete}
            handleEmail={this.handleEmail}
            handleLetter={this.handleLetter}
            redirect={this.redirect}
            disableOnClickOutside={!isSubheaderShow}
            breadcrumb={breadcrumb}
            notfound={notfound}
            query={query}
            entity={entity}
            dataId={dataId}
            windowId={windowId}
            viewId={query && query.viewId}
            siteName={siteName}
            editmode={editmode}
            handleEditModeToggle={handleEditModeToggle}
            activeTab={activeTab}
          />
        )}

        {showSidelist && isSideListShow && (
          <SideList
            windowType={windowId ? windowId : ''}
            closeOverlays={this.closeOverlays}
            closeSideList={this.handleSidelistToggle}
            isSideListShow={isSideListShow}
            disableOnClickOutside={!showSidelist}
            docId={dataId}
            defaultTab={sideListTab}
            open
          />
        )}

        {isEmailOpen && (
          <NewEmail
            windowId={windowId ? windowId : ''}
            docId={dataId}
            handleCloseEmail={this.handleCloseEmail}
          />
        )}
        {isLetterOpen && (
          <NewLetter
            windowId={windowId ? windowId : ''}
            docId={dataId}
            handleCloseLetter={this.handleCloseLetter}
          />
        )}
        <GlobalContextShortcuts
          handleSidelistToggle={this.handleSidelistToggle}
          handleMenuOverlay={
            isMenuOverlayShow
              ? () => this.handleMenuOverlay('', '')
              : () =>
                  this.closeOverlays('', () => this.handleMenuOverlay('', '0'))
          }
          handleInboxToggle={this.handleInboxToggle}
          handleUDToggle={this.handleUDToggle}
          openModal={
            dataId
              ? () => this.openModal(windowId, 'window', 'Advanced edit', true)
              : undefined
          }
          handlePrint={
            dataId
              ? () => this.handlePrint(windowId, dataId, docNoData.value)
              : undefined
          }
          handleEmail={this.handleEmail}
          handleLetter={this.handleLetter}
          handleDelete={dataId ? this.handleDelete : undefined}
          handleClone={
            dataId ? () => this.handleClone(windowId, dataId) : undefined
          }
          redirect={
            windowId
              ? () => this.redirect(`/window/${windowId}/new`)
              : undefined
          }
          handleDocStatusToggle={
            document.getElementsByClassName('js-dropdown-toggler')[0]
              ? this.handleDocStatusToggle
              : undefined
          }
          handleEditModeToggle={handleEditModeToggle}
          closeOverlays={this.closeOverlays}
        />
      </div>
    );
  }
}

/**
 * @typedef {object} Props Component props
 * @prop {*} activeTabId
 * @prop {*} breadcrumb
 * @prop {string} dataId
 * @prop {func} dispatch Dispatch function
 * @prop {object} inbox
 * @prop {func} me
 * @prop {*} childViewId
 * @prop {*} closeCallback
 * @prop {*} childViewSelectedIds
 * @prop {shape} data
 * @prop {*} docId
 * @prop {*} docNoData
 * @prop {*} docStatus
 * @prop {*} docStatusData
 * @prop {*} docSummaryData
 * @prop {*} dropzoneFocused
 * @prop {*} dataId
 * @prop {*} editmode
 * @prop {*} entity
 * @prop {*} handleDeletedStatus
 * @prop {*} handleEditModeToggle
 * @prop {string} indicator
 * @prop {shape} layout
 * @prop {bool} isAdvanced
 * @prop {bool} isDocumentNotSaved
 * @prop {bool} isNewDoc
 * @prop {string} staticModalType
 * @prop {*} modalTitle
 * @prop {*} modalType
 * @prop {*} modalSaveStatus
 * @prop {*} modalViewId
 * @prop {*} modalViewDocumentIds
 * @prop {*} notfound
 * @prop {string} staticModalType
 * @prop {string} tabId
 * @prop {*} parentSelection
 * @prop {*} parentType
 * @prop {*} parentViewId
 * @prop {*} parentViewSelectedIds
 * @prop {*} plugins
 * @prop {*} query
 * @prop {*} siteName
 * @prop {*} showIndicator
 * @prop {*} showSidelist
 * @prop {*} rawModalVisible
 * @prop {string} rowId
 * @prop {*} triggerField
 * @prop {*} viewId
 * @prop {*} windowId
 * @prop {*} windowType
 *  * @param {object} props Component props
 */
Header.propTypes = {
  dispatch: PropTypes.func.isRequired,
  inbox: PropTypes.object.isRequired,
  me: PropTypes.object.isRequired,
};

const mapStateToProps = state => ({
  inbox: state.appHandler.inbox,
  me: state.appHandler.me,
  pathname: state.routing.locationBeforeTransitions.pathname,
  plugins: state.pluginsHandler.files,
});

export default connect(mapStateToProps)(Header);
