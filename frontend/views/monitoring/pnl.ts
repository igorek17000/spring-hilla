import '@vaadin/button';
import '@vaadin/text-field';
import '@vaadin/number-field';
import '@vaadin/grid/vaadin-grid';
import {html} from 'lit';
import {customElement, state} from 'lit/decorators.js';
import {View} from 'Frontend/views/view';
import {Binder, field} from '@hilla/form';
import {getPnls} from 'Frontend/generated/MonitorEndpoint';
import PnlItem from 'Frontend/generated/com/example/application/bybit/monitor/dto/PnlItem';
import PnlItemModel from 'Frontend/generated/com/example/application/bybit/monitor/dto/PnlItemModel';

@customElement('pnl-view')
export class PnlView extends View {

  @state()
  private pnls1minutes: PnlItem[] = [];
  private pnls5minutes: PnlItem[] = [];
  private pnls15minutes: PnlItem[] = [];
  private binder = new Binder(this, PnlItemModel);

  render() {
    return html`
      <div class="p-m">
        <h3>1 minute</h3>
        <vaadin-grid .items="${this.pnls1minutes}" theme="row-stripes" style="max-width: 1500px">
<!--          <vaadin-grid-column path="minute"></vaadin-grid-column>-->
          <vaadin-grid-column path="contracts"></vaadin-grid-column>
          <vaadin-grid-column path="closing_direction"></vaadin-grid-column>
          <vaadin-grid-column path="qty"></vaadin-grid-column>
          <vaadin-grid-column path="entry_price"></vaadin-grid-column>
          <vaadin-grid-column path="exit_price"></vaadin-grid-column>
          <vaadin-grid-column path="closed_pnl"></vaadin-grid-column>
          <vaadin-grid-column path="exit_type"></vaadin-grid-column>
          <vaadin-grid-column path="trade_time"></vaadin-grid-column>
        </vaadin-grid>

        <h3>5 minute</h3>
        <vaadin-grid .items="${this.pnls5minutes}" theme="row-stripes" style="max-width: 1500px">
<!--          <vaadin-grid-column path="minute"></vaadin-grid-column>-->
          <vaadin-grid-column path="contracts"></vaadin-grid-column>
          <vaadin-grid-column path="closing_direction"></vaadin-grid-column>
          <vaadin-grid-column path="qty"></vaadin-grid-column>
          <vaadin-grid-column path="entry_price"></vaadin-grid-column>
          <vaadin-grid-column path="exit_price"></vaadin-grid-column>
          <vaadin-grid-column path="closed_pnl"></vaadin-grid-column>
          <vaadin-grid-column path="exit_type"></vaadin-grid-column>
          <vaadin-grid-column path="trade_time"></vaadin-grid-column>
        </vaadin-grid>

        <h3>15 minute</h3>
        <vaadin-grid .items="${this.pnls15minutes}" theme="row-stripes" style="max-width: 1500px">
<!--          <vaadin-grid-column path="minute"></vaadin-grid-column>-->
          <vaadin-grid-column path="contracts"></vaadin-grid-column>
          <vaadin-grid-column path="closing_direction"></vaadin-grid-column>
          <vaadin-grid-column path="qty"></vaadin-grid-column>
          <vaadin-grid-column path="entry_price"></vaadin-grid-column>
          <vaadin-grid-column path="exit_price"></vaadin-grid-column>
          <vaadin-grid-column path="closed_pnl"></vaadin-grid-column>
          <vaadin-grid-column path="exit_type"></vaadin-grid-column>
          <vaadin-grid-column path="trade_time"></vaadin-grid-column>
        </vaadin-grid>
      </div>
    `;
  }

  async firstUpdated() {
    this.pnls15minutes = await getPnls(1);
    this.pnls5minutes = await getPnls(5);
    this.pnls1minutes = await getPnls(15);
  }
}
