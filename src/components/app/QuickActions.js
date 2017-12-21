import counterpart from "counterpart";
import PropTypes from "prop-types";
import React, { Component } from "react";
import { connect } from "react-redux";

import { quickActionsRequest } from "../../actions/ViewActions";
import { openModal } from "../../actions/WindowActions";
import keymap from "../../shortcuts/keymap";
import QuickActionsContextShortcuts from "../shortcuts/QuickActionsContextShortcuts";
import Tooltips from "../tooltips/Tooltips.js";
import QuickActionsDropdown from "./QuickActionsDropdown";

const initialState = {
  actions: [],
  isDropdownOpen: false,
  isTooltip: false,
  loading: false
};

class QuickActions extends Component {
  static propTypes = {
    // from @connect
    dispatch: PropTypes.func.isRequired,

    // from <DocumentList>
    childView: PropTypes.object.isRequired,
    parentView: PropTypes.object.isRequired
  };

  constructor(props) {
    super(props);

    const {
      fetchOnInit,
      selected,
      windowType,
      viewId,
      childView,
      parentView
    } = props;
    this.state = initialState;

    if (fetchOnInit) {
      this.fetchActions(windowType, viewId, selected, childView, parentView);
    }
  }

  componentDidMount = () => {
    this.setState(initialState);
    this.mounted = true;
  };

  componentWillUnmount = () => {
    this.mounted = false;
  };

  componentWillReceiveProps = nextProps => {
    const { selected, viewId, windowType } = this.props;

    if (
      (nextProps.selected &&
        JSON.stringify(nextProps.selected) !== JSON.stringify(selected)) ||
      (nextProps.viewId && nextProps.viewId !== viewId) ||
      (nextProps.windowType && nextProps.windowType !== windowType)
    ) {
      this.fetchActions(
        nextProps.windowType,
        nextProps.viewId,
        nextProps.selected,
        nextProps.childView,
        nextProps.parentView
      );
    }
  };

  shouldComponentUpdate(nextProps) {
    return nextProps.shouldNotUpdate !== true;
  }

  updateActions = () => {
    const { windowType, viewId, selected, childView, parentView } = this.props;
    this.fetchActions(windowType, viewId, selected, childView, parentView);
  };

  componentDidUpdate = prevProps => {
    const { inBackground, inModal } = this.props;

    if (inModal === false && prevProps.inModal === true) {
      // gained focus after sub-modal closed
      this.setState({
        loading: false
      });
    }

    if (inBackground === true && prevProps.inBackground === false) {
      // gained focus after modal closed
      this.setState({
        loading: false
      });
    }
  };

  handleClickOutside = () => {
    this.toggleDropdown();
  };

  handleClick = action => {
    const { dispatch, viewId, selected, childView, parentView } = this.props;

    if (action.disabled) {
      return;
    }

    this.setState({
      loading: true
    });

    dispatch(
      openModal(
        action.caption,
        action.processId,
        "process",
        null,
        null,
        false,
        viewId,
        selected,
        null,
        null,
        parentView.viewId,
        parentView.viewSelectedIds,
        childView.viewId,
        childView.viewSelectedIds
      )
    );

    this.toggleDropdown();
  };

  fetchActions = (windowType, viewId, selected, childView, parentView) => {
    /*
        this.setState({
            loading: true
        });
*/

    if (windowType && viewId && selected && childView && parentView) {
      quickActionsRequest(windowType, viewId, selected, childView, parentView)
        .then(response => {
          this.setState({
            actions: response.data.actions,
            loading: false
          });
        })
        .catch(() => {
          this.setState({
            loading: false
          });
        });
    } else {
      this.setState({
        loading: false
      });
    }
  };

  toggleDropdown = option => {
    this.setState({
      isDropdownOpen: option
    });
  };

  toggleTooltip = visible => {
    this.setState({
      isTooltip: visible
    });
  };

  render() {
    const { actions, isDropdownOpen, isTooltip, loading } = this.state;

    const { shouldNotUpdate, processStatus, disabled } = this.props;

    const disabledDuringProcessing = processStatus === "pending" || loading;

    if (actions.length) {
      return (
        <div className={"js-not-unselect " + (disabled ? "disabled " : "")}>
          <span className="spacer-right">
            {counterpart.translate("window.quickActions.caption")}:
          </span>
          <div className="quick-actions-wrapper">
            <div
              className={
                "tag tag-success tag-xlg spacer-right " +
                "quick-actions-tag " +
                (actions[0].disabled || disabledDuringProcessing
                  ? "tag-default "
                  : "pointer ")
              }
              onClick={e => {
                e.preventDefault();

                this.handleClick(actions[0]);
              }}
              title={actions[0].caption}
            >
              {actions[0].caption}
            </div>
            <div
              className={
                "btn-meta-outline-secondary btn-icon-sm " +
                "btn-inline btn-icon pointer tooltip-parent " +
                (isDropdownOpen || disabledDuringProcessing
                  ? "btn-disabled "
                  : "")
              }
              onMouseEnter={() => this.toggleTooltip(true)}
              onMouseLeave={() => this.toggleTooltip(false)}
              onClick={() => {
                this.toggleDropdown(!isDropdownOpen);
              }}
            >
              <i className="meta-icon-down-1" />
              {isTooltip && (
                <Tooltips
                  name={keymap.QUICK_ACTION_TOGGLE}
                  action={"Toggle list"}
                  type={""}
                />
              )}
            </div>

            {isDropdownOpen && (
              <QuickActionsDropdown
                actions={actions}
                handleClick={this.handleClick}
                handleClickOutside={() => this.toggleDropdown(false)}
                disableOnClickOutside={!isDropdownOpen}
              />
            )}
          </div>
          <QuickActionsContextShortcuts
            handleClick={() =>
              shouldNotUpdate ? null : this.handleClick(actions[0])
            }
            onClick={() => this.toggleDropdown(!isDropdownOpen)}
          />
        </div>
      );
    } else {
      return false;
    }
  }
}

export default connect(false, false, false, { withRef: true })(QuickActions);
