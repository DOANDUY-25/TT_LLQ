import { NgModule } from '@angular/core';
import {
  TuiRoot,
  TuiButton,
  TuiIcon,
  TuiLoader,
  TuiTextfield,
  TuiError,
  TuiDataList,
  TuiLink,
  TuiNotification
} from '@taiga-ui/core';
import {
  TuiBadge,
  TuiStatus,
  TuiSwitch,
  TuiCheckbox,
  TuiPagination,
  TuiDataListWrapper,
  TuiSelect,
  TuiChip
} from '@taiga-ui/kit';

const TUI_MODULES: any[] = [
  TuiRoot,
  TuiButton,
  TuiIcon,
  TuiLoader,
  TuiTextfield,
  TuiError,
  TuiDataList,
  TuiLink,
  TuiNotification,
  TuiBadge,
  TuiStatus,
  TuiSwitch,
  TuiCheckbox,
  TuiPagination,
  TuiDataListWrapper,
  TuiSelect,
  TuiChip
];

@NgModule({
  imports: TUI_MODULES,
  exports: TUI_MODULES
})
export class TaigaUiModule {}
