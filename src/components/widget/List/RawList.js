import React, { Component } from "react";
import ReactCSSTransitionGroup from "react-addons-css-transition-group";

let lastKeyWasTab = false;

class RawList extends Component {
  isFocused = false;
  considerBlur = false;

  constructor(props) {
    super(props);

    this.state = {
      selected: props.selected || 0,
      dropdownList: props.list || [],
      isOpen: false
    };
  }

  componentWillMount() {
    window.addEventListener("keydown", this.handleTab);
    window.addEventListener("click", this.handleTab);
  }

  componentDidMount = () => {
    const { autofocus, onRequestListData } = this.props;
    if (this.dropdown && autofocus && onRequestListData) {
      onRequestListData();
    }
  };

  componentDidUpdate = (prevProps, prevState) => {
    const {
      list,
      mandatory,
      defaultValue,
      autofocus,
      blur,
      property,
      initialFocus,
      selected,
      doNotOpenOnFocus,
      lastProperty,
      loading,
      disableAutofocus
    } = this.props;

    if (prevProps.blur !== blur) {
      if (blur) {
        this.handleBlur();
      }
    }

    if (
      list.length === 0 &&
      prevProps.loading !== loading &&
      loading === false &&
      lastProperty
    ) {
      disableAutofocus();
    }

    if (this.dropdown && autofocus) {
      if (prevState.selected !== this.state.selected) {
        if (list.length === 1) {
          this.handleSelect(list[0]);
        }

        if (!doNotOpenOnFocus && list.length > 1) {
          this.setState({
            isOpen: true
          });
        }
      }
    }

    if (this.dropdown) {
      if (autofocus) {
        if (list && list.length > 0) {
          this.dropdown.focus();
        }
      } else {
        if (property && prevProps.defaultValue !== defaultValue) {
          this.dropdown.focus();
        } else {
          if (initialFocus && !defaultValue) {
            this.dropdown.focus();
          }
        }
      }
    }

    if (prevProps.list !== list) {
      let dropdown = [];

      if (!mandatory) {
        dropdown.push(0);
      }

      if (list.length > 0) {
        let openDropdownState = {};

        if (this.openDropdown && list.length > 1) {
          this.openDropdown = false;
          openDropdownState.isOpen = true;
        }

        let dropdownList = dropdown.concat(list);

        this.setState(
          Object.assign(
            {
              dropdownList: dropdownList,
              selected: defaultValue ? defaultValue : list[0]
            },
            openDropdownState
          )
        );
      }
    }

    if (prevProps.selected !== selected) {
      this.setState({
        selected: selected
      });
    }

    const { isOpen } = this.state;

    // trigger handleBlur action if dropdown is still opened
    // after focus is lost
    if (!this.isFocused && isOpen) {
      this.handleBlur();
    }

    // no need for updating scroll
    if (!this.isFocused || !isOpen || !list.length) {
      return;
    }

    const { listScroll, items } = this;

    const listElHeight = this.optionElement.offsetHeight;
    const listVisible = Math.floor(listScroll.offsetHeight / listElHeight);
    const shouldListScrollUpdate = listVisible <= items.childNodes.length;

    if (!shouldListScrollUpdate) {
      return;
    }

    const selectedIndex = this.getSelectedIndex();
    const visibleMin = listScroll.scrollTop;
    const visibleMax = visibleMin + listVisible * listElHeight;

    //not visible from down
    const scrollFromUp = listElHeight * (selectedIndex - listVisible + 1);

    if (
      (selectedIndex + 1) * listElHeight > visibleMax &&
      listScroll.scrollTop !== scrollFromUp
    ) {
      listScroll.scrollTop = scrollFromUp;
      return;
    }

    //not visible from above
    const scrollFromDown = selectedIndex * listElHeight;

    if (
      selectedIndex * listElHeight < visibleMin &&
      listScroll.scrollTop !== scrollFromDown
    ) {
      listScroll.scrollTop = scrollFromDown;
    }
  };

  componentWillUnmount() {
    window.removeEventListener("keydown", this.handleTab);
    window.removeEventListener("click", this.handleTab);
  }

  focus = () => {
    if (this.dropdown) {
      this.dropdown.focus();
    }
  };

  openDropdownList = () => {
    this.setState(
      {
        isOpen: true
      },
      () => {
        this.focus();
      }
    );
  };

  closeDropdownList = () => {
    if (this.state && this.state.isOpen) {
      this.setState({
        isOpen: false
      });
    }
  };

  getSelectedIndex() {
    const { list, mandatory } = this.props;
    const { selected } = this.state;

    if (selected === 0) {
      return 0;
    }

    let baseIndex = list.indexOf(selected);
    if (selected && baseIndex < 0) {
      baseIndex = list.findIndex(item => item.key === selected.key);
    }

    if (!mandatory) {
      return baseIndex + 1;
    }

    return baseIndex;
  }

  navigateToAlphanumeric = char => {
    const { list } = this.props;
    const { isOpen, selected } = this.state;

    if (!isOpen) {
      this.setState({
        isOpen: true
      });
    }

    const items = list.filter(
      item => item.caption.toUpperCase() === char.toUpperCase()
    );

    const selectedIndex = items.indexOf(selected);
    const item = selectedIndex > -1 ? items[selectedIndex + 1] : items[0];

    if (!item) {
      return;
    }

    this.setState({
      selected: item
    });
  };

  navigate = up => {
    const { selected, dropdownList, isOpen } = this.state;

    if (!isOpen) {
      this.setState({
        isOpen: true
      });
    }

    let selectedIndex = null;

    dropdownList.map((item, index) => {
      if (JSON.stringify(item) === JSON.stringify(selected)) {
        selectedIndex = index;
      }
    });

    const next = up ? selectedIndex + 1 : selectedIndex - 1;

    this.setState({
      selected:
        next >= 0 && next <= dropdownList.length - 1
          ? dropdownList[next]
          : selected
    });
  };

  handleBlur = () => {
    if (!this.considerBlur) {
      return;
    }

    this.considerBlur = false;

    const { selected, doNotOpenOnFocus } = this.props;

    this.isFocused = false;

    if (!doNotOpenOnFocus && this.dropdown) {
      this.dropdown.blur();
    }

    this.setState({
      isOpen: false,
      selected: selected || 0
    });
  };

  /*
     * Alternative method to open dropdown, in case of disabled opening
     * on focus.
     */
  handleClick = e => {
    this.considerBlur = true;

    e.preventDefault();

    const { onFocus } = this.props;

    onFocus && onFocus();

    this.setState({
      isOpen: true
    });
  };

  handleFocus = event => {
    this.considerBlur = this.considerBlur || lastKeyWasTab;
    this.isFocused = true;

    if (event) {
      event.preventDefault();
    }

    const { onFocus, doNotOpenOnFocus, autofocus } = this.props;

    onFocus && onFocus();

    if (!doNotOpenOnFocus && !autofocus) {
      this.openDropdown = true;
    }
  };

  handleChange = e => {
    e.preventDefault();

    this.handleBlur();
  };

  handleSelect = option => {
    this.considerBlur = true;

    const { onSelect } = this.props;

    if (option.key === null) {
      onSelect(null);
    } else {
      onSelect(option);
    }

    this.setState(
      {
        selected: option || 0
      },
      () => this.handleBlur()
    );
  };

  handleSwitch = option => {
    this.setState({
      selected: option || 0
    });
  };

  handleKeyDown = e => {
    const { onSelect, list, readonly } = this.props;
    const { selected, isOpen } = this.state;

    if (e.keyCode > 47 && e.keyCode < 123) {
      this.navigateToAlphanumeric(e.key);
    } else {
      switch (e.key) {
        case "ArrowDown":
          e.preventDefault();
          this.navigate(true);
          break;

        case "ArrowUp":
          e.preventDefault();
          this.navigate(false);
          break;
        case "Enter":
          e.preventDefault();

          if (isOpen) {
            e.stopPropagation();
          }

          if (selected) {
            this.isFocused = true;
            this.handleSelect(selected);
          } else {
            onSelect(null);
          }

          break;

        case "Escape":
          e.preventDefault();
          this.handleBlur();
          break;

        case "Tab":
          list.length === 0 && !readonly && onSelect(null);
          break;
      }
    }
  };

  handleTab = event => {
    lastKeyWasTab = event.key == "Tab";
  };

  getRow = (option, index) => {
    const { defaultValue } = this.props;
    const { selected } = this.state;

    const value = defaultValue ? defaultValue.caption : null;

    const classes = ["input-dropdown-list-option"];

    if (selected != null && selected !== 0) {
      if (
        selected.key === option.key ||
        (!selected && (value === option.caption || (!value && index === 0)))
      ) {
        classes.push("input-dropdown-list-option-key-on");
      }
    }

    return (
      <div
        key={option.key}
        className={classes.join(" ")}
        onMouseEnter={() => this.handleSwitch(option)}
        onClick={() => this.handleSelect(option)}
        ref={option => (this.optionElement = option)}
      >
        <p className="input-dropdown-item-title">{option.caption}</p>
      </div>
    );
  };

  renderOptions = () => {
    const { list, mandatory, emptyText } = this.props;

    let emptyRow;

    if (!mandatory && emptyText) {
      emptyRow = this.getRow({ key: null, caption: emptyText });
    }

    return (
      <div
        ref={ref => {
          this.items = ref;
        }}
      >
        {/* if field is not mandatory add extra empty row */}
        {emptyRow}
        {list.map(this.getRow)}
      </div>
    );
  };

  render() {
    const {
      list,
      rank,
      readonly,
      defaultValue,
      selected,
      align,
      updated,
      loading,
      rowId,
      isModal,
      tabIndex,
      disabled,
      mandatory,
      validStatus,
      lookupList
    } = this.props;

    let placeholder = "";
    const isListEmpty = list.length === 0;
    const { isOpen } = this.state;

    if (typeof defaultValue === "string") {
      placeholder = defaultValue;
    } else {
      placeholder = defaultValue && defaultValue.caption;
    }

    let value;

    if (lookupList) {
      value = placeholder;
    } else if (selected) {
      value = selected.caption;
    }

    if (!value) {
      value = "";
    }

    return (
      <div
        ref={c => (this.dropdown = c)}
        className={
          "input-dropdown-container " +
          (readonly ? "input-disabled " : "") +
          (rowId ? "input-dropdown-container-static " : "") +
          (rowId && !isModal ? "input-table " : "")
        }
        tabIndex={tabIndex ? tabIndex : 0}
        onFocus={readonly ? null : this.handleFocus}
        onBlur={this.handleBlur}
        onClick={readonly ? null : this.handleClick}
        onKeyDown={this.handleKeyDown}
      >
        <div
          className={
            "input-dropdown input-block input-readonly input-" +
            (rank ? rank : "secondary") +
            (updated ? " pulse " : " ") +
            (mandatory && !selected ? "input-mandatory " : "") +
            (validStatus &&
            (!validStatus.valid && !validStatus.initialValue) &&
            !isOpen
              ? "input-error "
              : "")
          }
        >
          <div
            className={
              "input-editable input-dropdown-focused " +
              (align ? "text-xs-" + align + " " : "")
            }
          >
            <input
              ref={c => (this.inputSearch = c)}
              type="text"
              className={
                "input-field js-input-field " +
                "font-weight-semibold " +
                (disabled ? "input-disabled " : "")
              }
              readOnly
              tabIndex={-1}
              placeholder={placeholder}
              value={value}
              disabled={readonly || disabled}
              onChange={this.handleChange}
            />
          </div>

          <div className="input-icon">
            <i className="meta-icon-down-1 input-icon-sm" />
          </div>
        </div>
        {this.isFocused &&
          isOpen && (
            <div
              className="input-dropdown-list"
              ref={ref => {
                this.listScroll = ref;
              }}
            >
              {isListEmpty &&
                loading === false && (
                  <div className="input-dropdown-list-header">
                    There is no choice available
                  </div>
                )}
              {loading &&
                isListEmpty && (
                  <div className="input-dropdown-list-header">
                    <ReactCSSTransitionGroup
                      transitionName="rotate"
                      transitionEnterTimeout={1000}
                      transitionLeaveTimeout={1000}
                    >
                      <div className="rotate icon-rotate">
                        <i className="meta-icon-settings" />
                      </div>
                    </ReactCSSTransitionGroup>
                  </div>
                )}
              {this.renderOptions()}
            </div>
          )}
      </div>
    );
  }
}

export default RawList;
