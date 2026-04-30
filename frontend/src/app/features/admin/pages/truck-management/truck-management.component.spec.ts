import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TruckManagementComponent } from './truck-management.component';

describe('TruckManagementComponent', () => {
  let component: TruckManagementComponent;
  let fixture: ComponentFixture<TruckManagementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TruckManagementComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TruckManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
