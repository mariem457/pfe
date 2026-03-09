import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BinsComponent } from './bins.component';

describe('BinsComponent', () => {
  let component: BinsComponent;
  let fixture: ComponentFixture<BinsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BinsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BinsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
