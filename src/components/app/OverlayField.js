import React, { Component } from 'react';
import counterpart from 'counterpart';
import PropTypes from 'prop-types';
import onClickOutside from 'react-onclickoutside';

import MasterWidget from '../widget/MasterWidget';
import RawWidget from '../widget/RawWidget';
import BarcodeScanner from '../widget/BarcodeScanner/BarcodeScannerWidget';

class OverlayField extends Component {
  handleClickOutside = () => {
    const { closeOverlay } = this.props;

    closeOverlay();
  };

  handleKeyDown = e => {
    const { handleSubmit, closeOverlay, onSelectBarcode } = this.props;

    switch (e.key) {
      case 'Enter':
        handleSubmit();
        break;
      case 'Escape':
        closeOverlay();
        break;
      default:
        onSelectBarcode(null);
        break;
    }
  };

  renderBarcodeScanButton = () => {
    const { onScanBarcode } = this.props;
    console.log("onScanBarcode: "+onScanBarcode);

    return (
      <button
        className="btn btn-sm btn-meta-success"
        onClick={() => onScanBarcode(true)}
      >
        {counterpart.translate('widget.scanFromCamera.caption')}
      </button>
    );
  };

  renderElements = (layout, data, type) => {
    const { disabled, codeSelected, onChange } = this.props;
    const elements = layout.elements;

    console.log("onChange: "+onChange);

    return elements.map((elem, id) => {
      const widgetData = elem.fields.map(item => data[item.field] || -1);
      let captionElement = null;

      if (elem.barcodeScannerType) {
        captionElement = this.renderBarcodeScanButton();
      }

      return (
        <MasterWidget
          entity="process"
          key={'element' + id}
          windowType={type}
          dataId={layout.pinstanceId}
          widgetData={widgetData}
          isModal={true}
          disabled={disabled}
          autoFocus={id === 0}
          captionElement={captionElement}
          data={codeSelected || undefined}
          onChange={onChange}
          {...elem}
        />
      );
    });
  };

  renderParameters = layout => {
    const {
      windowType,
      viewId,
      onShow,
      onHide,
      handlePatch,
      handleChange,
      captionValue,
      codeSelected,
    } = this.props;
    const parameters = layout.parameters;
    return parameters.map((item, index) => {
      let captionElement = null;

      if (item.barcodeScannerType) {
        captionElement = this.renderBarcodeScanButton();
      }

      if (codeSelected) {
        item.value = codeSelected;
      }

      return (
        <RawWidget
          defaultValue={captionValue}
          captionElement={captionElement}
          entity="documentView"
          subentity="filter"
          subentityId={layout.filterId}
          widgetType={item.widgetType}
          fields={[item]}
          type={item.type}
          widgetData={[item]}
          key={index}
          id={index}
          range={item.range}
          caption={item.caption}
          noLabel={false}
          filterWidget={true}
          autoFocus={index === 0}
          textSelected={true}
          {...{
            handlePatch,
            handleChange,
            windowType,
            onShow,
            onHide,
            viewId,
          }}
        />
      );
    });
  };

  render() {
    const { data, layout, type, filter, scanning, scannerElement } = this.props;
    let renderedContent = null;

    if (scanning) {
      renderedContent = scannerElement;
    } else {
      // TODO: Why sometimes it's wrapped in MasterWidget, and other
      // times it's not ? Needs refactoring.
      if (filter) {
        renderedContent = this.renderParameters(layout);
      } else if (!filter && layout && layout.elements) {
        renderedContent = this.renderElements(layout, data, type);
      }
    }

    return (
      <div
        className="overlay-field"
        onKeyDown={e => this.handleKeyDown(e)}
        tabIndex={-1}
      >
        {renderedContent}
      </div>
    );
  }
}

OverlayField.propTypes = {
  onChange: PropTypes.func,
  closeOverlay: PropTypes.func,
};

export default BarcodeScanner(onClickOutside(OverlayField));
