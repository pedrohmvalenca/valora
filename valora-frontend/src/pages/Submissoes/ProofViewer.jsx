import { useEffect, useState } from "react";
import { FileText } from "lucide-react";

import { getSubmissionProof } from "@/services/submissions";

export default function ProofViewer({ submissionId, proofPath }) {
  const [state, setState] = useState({ status: "loading", url: null, contentType: null });

  useEffect(() => {
    let active = true;
    let objectUrl = null;
    setState({ status: "loading", url: null, contentType: null });
    getSubmissionProof(submissionId)
      .then(({ url, contentType }) => {
        if (!active) {
          URL.revokeObjectURL(url);
          return;
        }
        objectUrl = url;
        setState({ status: "ready", url, contentType });
      })
      .catch(() => {
        if (active) setState({ status: "error", url: null, contentType: null });
      });
    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [submissionId]);

  const isImage = state.contentType?.startsWith("image/");
  const isPdf = state.contentType === "application/pdf";

  return (
    <div>
      <p className="text-xs uppercase text-muted-foreground mb-1">Comprovante</p>

      {state.status === "loading" && (
        <div className="border border-border rounded-md p-4 bg-muted/30 text-sm text-muted-foreground">
          Carregando comprovante…
        </div>
      )}

      {state.status === "error" && (
        <div className="border border-destructive/30 rounded-md p-4 bg-destructive/5 flex items-center gap-3">
          <FileText className="h-6 w-6 text-destructive shrink-0" />
          <div className="min-w-0">
            <p className="text-sm font-medium text-destructive">Não foi possível carregar o comprovante</p>
            <p className="text-xs text-muted-foreground font-mono truncate">{proofPath}</p>
          </div>
        </div>
      )}

      {state.status === "ready" && isImage && (
        <a
          href={state.url}
          target="_blank"
          rel="noopener noreferrer"
          className="block border border-border rounded-md overflow-hidden hover:border-primary/40 transition-colors"
        >
          <img
            src={state.url}
            alt="Comprovante da submissão"
            className="w-full max-h-96 object-contain bg-muted/30"
          />
        </a>
      )}

      {state.status === "ready" && !isImage && (
        <a
          href={state.url}
          target="_blank"
          rel="noopener noreferrer"
          download={isPdf ? undefined : proofPath}
          className="border border-border rounded-md p-4 bg-muted/30 flex items-center gap-3 hover:border-primary/40 transition-colors"
        >
          <FileText className="h-6 w-6 text-muted-foreground shrink-0" />
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium">{isPdf ? "Comprovante PDF" : "Comprovante"}</p>
            <p className="text-xs text-muted-foreground font-mono truncate">
              {proofPath} · Abrir em nova aba
            </p>
          </div>
        </a>
      )}
    </div>
  );
}
