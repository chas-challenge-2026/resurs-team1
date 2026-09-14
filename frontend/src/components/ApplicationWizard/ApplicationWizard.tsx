/** Everything the customer fills in + autofilled*/
export interface ApplicationFormData {
  // auto filled:
  orgNumber: string;
  companyName: string;

  // filled out during wizard:
  contactName: string;
  email: string;
  phoneNumber: string;

  // becomes a union once the backend hands over the enum values -- A | B | C
  purpose: string;
  // TODO: requestedAmount in api rn, until they change according to request in slack
  loanAmount: number;
  repaymentPeriod: number;
}

interface ApplicationWizardProps {
  /** Which step to render. Owned by the page. */
  step: number;
  /** Current answers. Owned by the page. */
  values: ApplicationFormData;
  /** Report a change upwards. Partial, so a field can be sent on its own. */
  onChange: (patch: Partial<ApplicationFormData>) => void;
}

const ApplicationWizard = ({ step }: ApplicationWizardProps) => {
  switch (step) {
    case 2:
      return <p>steg 2</p>

    default:
      return null
  }
}

export default ApplicationWizard
