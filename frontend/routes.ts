import { Route } from '@vaadin/router';
import './views/monitoring/balance';
import './views/monitoring/pnl';
import './views/monitoring/execute';
import './views/main-layout';

export type ViewRoute = Route & {
  title?: string;
  icon?: string;
  children?: ViewRoute[];
};

export const views: ViewRoute[] = [
  // place routes below (more info https://hilla.dev/docs/routing)
  {
    path: '',
    component: 'balance-view',
    icon: '',
    title: '',
  },
  {
    path: 'balance',
    component: 'balance-view',
    icon: 'la la-list-alt',
    title: 'Balance',
  },
    // TODO TRACE 데이터 생기면 주석 제거
  // {
  //   path: 'pnl',
  //   component: 'pnl-view',
  //   icon: 'la la-list-alt',
  //   title: 'Closed P&L',
  // },
  {
    path: 'execute',
    component: 'execute-view',
    icon: 'la la-list-alt',
    title: 'Execute List',
  },
];
export const routes: ViewRoute[] = [
  {
    path: '',
    component: 'main-layout',
    children: [...views],
  },
];
