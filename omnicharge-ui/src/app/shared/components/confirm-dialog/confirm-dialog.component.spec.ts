import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ConfirmDialogComponent } from './confirm-dialog.component';
import { ConfirmDialogService } from './confirm-dialog.service';

describe('ConfirmDialogComponent', () => {
  let component: ConfirmDialogComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .overrideComponent(ConfirmDialogComponent, { set: { template: '<div></div>', imports: [], schemas: [NO_ERRORS_SCHEMA] } })
    .compileComponents();

    const fixture = TestBed.createComponent(ConfirmDialogComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have dialogService injected', () => {
    expect(component.dialogService).toBeTruthy();
    expect(component.dialogService.config()).toBeNull();
  });

  it('should show config when dialog is opened', () => {
    component.dialogService.open({ title: 'Delete?', message: 'Are you sure?' });
    expect(component.dialogService.config()?.title).toBe('Delete?');
  });
});
