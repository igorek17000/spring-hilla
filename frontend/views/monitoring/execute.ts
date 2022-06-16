import '@vaadin/button';
import '@vaadin/text-field';
import '@vaadin/number-field';
import '@vaadin/grid/vaadin-grid';
import {html} from 'lit';
import {customElement, state} from 'lit/decorators.js';
import {View} from 'Frontend/views/view';
import {Binder, field} from '@hilla/form';
import {getExecute} from 'Frontend/generated/MonitorEndpoint';
import PnlItem from 'Frontend/generated/com/example/application/bybit/monitor/dto/PnlItem';
import PnlItemModel from 'Frontend/generated/com/example/application/bybit/monitor/dto/PnlItemModel';

@customElement('execute-view')
export class ExecuteView extends View {

  @state()
  private execute1minutes: PnlItem[] = [];
  private execute5minutes: PnlItem[] = [];
  private execute15minutes: PnlItem[] = [];
  private binder = new Binder(this, PnlItemModel);

  render() {
    return html`
      <div class="p-m">
        <h3>1 minute</h3>
        <vaadin-grid .items="${this.execute1minutes}" theme="row-stripes" style="max-width: 1500px">
<!--          <vaadin-grid-column path="minute"></vaadin-grid-column>-->
          <vaadin-grid-column path="contracts" ></vaadin-grid-column>
          <vaadin-grid-column path="execute_type"></vaadin-grid-column>
          <vaadin-grid-column path="qty"></vaadin-grid-column>
          <vaadin-grid-column path="order_price"></vaadin-grid-column>
          <vaadin-grid-column path="exit_type"></vaadin-grid-column>
          <vaadin-grid-column path="trade_time"></vaadin-grid-column>
        </vaadin-grid>

        <h3>5 minute</h3>
        <vaadin-grid .items="${this.execute5minutes}" theme="row-stripes" style="max-width: 1500px">
<!--          <vaadin-grid-column path="minute"></vaadin-grid-column>-->
          <vaadin-grid-column path="contracts"></vaadin-grid-column>
          <vaadin-grid-column path="execute_type"></vaadin-grid-column>
          <vaadin-grid-column path="qty"></vaadin-grid-column>
          <vaadin-grid-column path="order_price"></vaadin-grid-column>
          <vaadin-grid-column path="exit_type"></vaadin-grid-column>
          <vaadin-grid-column path="trade_time"></vaadin-grid-column>
        </vaadin-grid>

        <h3>15 minute</h3>
        <vaadin-grid .items="${this.execute15minutes}" theme="row-stripes" style="max-width: 1500px">
<!--          <vaadin-grid-column path="minute"></vaadin-grid-column>-->
          <vaadin-grid-column path="contracts"></vaadin-grid-column>
          <vaadin-grid-column path="execute_type"></vaadin-grid-column>
          <vaadin-grid-column path="qty"></vaadin-grid-column>
          <vaadin-grid-column path="order_price"></vaadin-grid-column>
          <vaadin-grid-column path="exit_type"></vaadin-grid-column>
          <vaadin-grid-column path="trade_time"></vaadin-grid-column>
        </vaadin-grid>
      </div>
    `;
  }

  async firstUpdated() {
    this.execute15minutes = await getExecute(1);
    this.execute5minutes = await getExecute(5);
    this.execute1minutes = await getExecute(15);
  }
}
