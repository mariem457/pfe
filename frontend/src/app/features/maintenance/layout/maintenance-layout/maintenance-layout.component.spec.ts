import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MaintenanceLayoutComponent } from './maintenance-layout.component';

describe('MaintenanceLayoutComponent', () => {
  let component: MaintenanceLayoutComponent;
  let fixture: ComponentFixture<MaintenanceLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MaintenanceLayoutComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MaintenanceLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
