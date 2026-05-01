import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChauffeurLayoutComponent } from './chauffeur-layout.component';

describe('LayoutComponent', () => {
  let component: ChauffeurLayoutComponent;
  let fixture: ComponentFixture<ChauffeurLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChauffeurLayoutComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChauffeurLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
