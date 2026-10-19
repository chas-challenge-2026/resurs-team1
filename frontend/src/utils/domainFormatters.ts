import { PURPOSE_OPTIONS } from "../constants/constants";

export const formatReferenceNumber = (id: number) => {
  return `REF-${id}`
}

export function formatFilename(filename: string): string {
  return filename.replace(/^\d+_/, "");
}

export function getPurposeLabel(purpose: string): string {
  const option = PURPOSE_OPTIONS.find((opt) => opt.value === purpose);
  return option ? option.label : purpose;
}