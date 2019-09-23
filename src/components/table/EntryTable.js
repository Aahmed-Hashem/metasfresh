import React, { Component } from 'react';
import classnames from 'classnames';

import WidgetTooltip from '../widget/WidgetTooltip';
import MasterWidget from '../widget/MasterWidget';

/**
 * @file Class based component.
 * @module EntryTable
 * @extends Component
 */
export default class EntryTable extends Component {
  constructor(props) {
    super(props);

    this.state = {
      tooltipToggled: null,
    };
  }

  /**
   * @method widgetTooltipToggle
   * @summary ToDo: Describe the method
   * @param {*} field
   * @param {*} value
   * @todo Write the documentation
   */
  widgetTooltipToggle = (field, value) => {
    const curVal = this.state.tooltipToggled;
    let newVal = field;

    if (value === false || field === curVal) {
      newVal = null;
    }

    this.setState({
      tooltipToggled: newVal,
    });
  };

  /**
   * @method renderElements
   * @summary ToDo: Describe the method
   * @param {*} elements
   * @param {*} columnsCount
   * @todo Write the documentation
   */
  renderElements = (elements, columnsCount) => {
    const {
      data,
      rowData,
      extendedData,
      addRefToWidgets,
      handleBlurWidget,
      layout,
      dataId,
      tabIndex,
      fullScreen,
    } = this.props;
    const { tooltipToggled } = this.state;
    const renderedArray = [];
    const colWidth = Math.floor(12 / columnsCount);

    if (rowData && rowData.size) {
      for (let i = 0; i < columnsCount; i += 1) {
        const elem = elements.cols[i];

        if (elem && elem.fields && elem.fields.length) {
          const fieldName = elem.fields ? elem.fields[0].field : '';
          const widgetData = [rowData.get(0).fieldsByName[fieldName]];
          const relativeDocId = data.ID && data.ID.value;
          let tooltipData = null;
          let tooltipWidget = elem.fields
            ? elem.fields.find(field => {
                if (field.type === 'Tooltip') {
                  tooltipData = rowData.get(0).fieldsByName[field.field];

                  if (tooltipData && tooltipData.value) {
                    return field;
                  }
                }
                return false;
              })
            : null;

          renderedArray.push(
            <td
              key={`${fieldName}-cell-${i}`}
              className={classnames(
                `col-sm-${colWidth}`,
                {
                  [`text-${widgetData.gridAlign}`]: widgetData.gridAlign,
                  'cell-disabled': widgetData[0].readonly,
                  'cell-mandatory': widgetData[0].mandatory,
                },
                `field-${widgetData[0].widgetType}`
              )}
            >
              <MasterWidget
                ref={addRefToWidgets}
                entity="window"
                windowType={layout.windowId}
                dataId={dataId}
                dataEntry={true}
                fieldName={fieldName}
                widgetData={widgetData}
                isModal={false}
                tabId={extendedData.tabId}
                rowId={dataId}
                relativeDocId={relativeDocId}
                isAdvanced={false}
                tabIndex={tabIndex}
                fullScreen={fullScreen}
                onBlurWidget={handleBlurWidget}
                {...elem}
              />
              {tooltipWidget && (
                <WidgetTooltip
                  widget={tooltipWidget}
                  data={tooltipData}
                  fieldName={fieldName}
                  isToggled={tooltipToggled === fieldName}
                  onToggle={this.widgetTooltipToggle}
                />
              )}
            </td>
          );
        } else {
          renderedArray.push(<td key={`__-cell-${i}`} />);
        }
      }
      return renderedArray;
    }

    return null;
  };

  /**
   * @method render
   * @summary ToDo: Describe the method
   * @todo Write the documentation
   */
  render() {
    const { rows } = this.props;

    return (
      <table className="table js-table layout-fix">
        <tbody>
          {rows.map((cols, idx) => {
            return (
              <tr className="table-row" key={`entry-row-${idx}`}>
                {this.renderElements(cols, cols.colsCount)}
              </tr>
            );
          })}
        </tbody>
      </table>
    );
  }
}
