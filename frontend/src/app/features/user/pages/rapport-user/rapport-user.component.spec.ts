import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RapportUserComponent } from './rapport-user.component';

describe('RapportUserComponent', () => {
  let component: RapportUserComponent;
  let fixture: ComponentFixture<RapportUserComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RapportUserComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RapportUserComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
