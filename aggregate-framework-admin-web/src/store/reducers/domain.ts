import {Domain} from '../actions/domain';
import {CommonAction} from 'app-common';

export interface DomainState {
  domainData: any;
  currentDomain: string;
  refresh: number;
}

const defaultState: DomainState = {
  domainData: [],
  currentDomain: '',
  refresh: 0
};

export default function domain(state = defaultState, {type, payload}: CommonAction) {
  switch (type) {
    case Domain.UPDATE_DOMAIN_DATA:
      return {...state, domainData: payload};
    case Domain.UPDATE_CURRENT_DOMAIN:
      return {...state, currentDomain: payload, refresh: state.refresh + 1};
    default:
      return state;
  }
}
