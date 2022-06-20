import '@vaadin/button';
import '@vaadin/text-field';
import '@vaadin/number-field';
import '@vaadin/grid/vaadin-grid';
import {html} from 'lit';
import {customElement, state} from 'lit/decorators.js';
import {View} from 'Frontend/views/view';
import {Binder, field} from '@hilla/form';
import {getBalance} from 'Frontend/generated/MonitorEndpoint';
import BalanceItem from 'Frontend/generated/com/example/application/bybit/monitor/dto/BalanceItem';
import BalanceItemModel from 'Frontend/generated/com/example/application/bybit/monitor/dto/BalanceItemModel';

@customElement('balance-view')
export class BalanceView extends View {

  @state()
  private balances: BalanceItem[] = [];
  private binder = new Binder(this, BalanceItemModel);

  render() {
    return html`
            <div class="p-m">
             <h3>잔액</h3>
             <vaadin-grid .items="${this.balances}" theme="row-stripes" style="max-width: 800px">
              <vaadin-grid-column header="분봉" width="200px" path="minute" ></vaadin-grid-column>
              <vaadin-grid-column header="비트코인(BTC)" width="200px" path="btc"></vaadin-grid-column>
              <vaadin-grid-column header="달러(USD)" width="200px" path="usd"></vaadin-grid-column>
               <vaadin-grid-column header="원화(KRW)" width="200px" path="won"></vaadin-grid-column>
             </vaadin-grid>
            </div>
    `;
  }

  async firstUpdated() {
    this.balances = await getBalance()
    window.setInterval(async () => {
      this.balances = await getBalance()
    },5000)
  }

}
