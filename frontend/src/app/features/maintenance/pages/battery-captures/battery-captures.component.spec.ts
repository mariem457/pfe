import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BatteryCapturesComponent } from './battery-captures.component';

describe('BatteryCapturesComponent', () => {
  let component: BatteryCapturesComponent;
  let fixture: ComponentFixture<BatteryCapturesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BatteryCapturesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BatteryCapturesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
