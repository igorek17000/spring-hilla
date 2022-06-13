import { Route } from '@vaadin/router';
import './views/grocery/grocery-view';
import './views/monitoring/balance';
import './views/monitoring/pnl';
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
    component: 'grocery-view',
    icon: '',
    title: '',
  },
  {
    path: 'grocery',
    component: 'grocery-view',
    icon: 'la la-list-alt',
    title: 'Grocery',
  },
  {
    path: 'balance',
    component: 'balance-view',
    icon: 'la la-list-alt',
    title: 'Balance',
  },
  {
    path: 'pnl',
    component: 'pnl-view',
    icon: 'la la-list-alt',
    title: 'P&L',
  },
];
export const routes: ViewRoute[] = [
  {
    path: '',
    component: 'main-layout',
    children: [...views],
  },
];
