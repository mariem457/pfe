import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OptimisationComponent } from './optimisation.component';

describe('OptimisationComponent', () => {
  let component: OptimisationComponent;
  let fixture: ComponentFixture<OptimisationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OptimisationComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OptimisationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
