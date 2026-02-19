import {
  useEffect,
  useMemo,
  useState,
  type ButtonHTMLAttributes,
  type FormEvent,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
} from 'react'

type VoucherPublic = {
  code: string
  discountType: 'PERCENT' | 'FIXED'
  discountValue: number
  minSpend: number | null
  quotaRemaining: number
  startAt: string
  endAt: string
}

type ValidateVoucherResponse = {
  valid: boolean
  code: string
  orderAmount: number
  discountAmount: number | null
  message: string
}

type ClaimVoucherResponse = {
  success: boolean
  idempotent: boolean
  code: string
  orderId: string
  orderAmount: number
  discountApplied: number | null
  quotaRemaining: number | null
  message: string
}

type CreateVoucherResponse = {
  id: number
  code: string
  discountType: 'PERCENT' | 'FIXED'
  discountValue: number
  startAt: string
  endAt: string
  minSpend: number | null
  quotaTotal: number
  quotaRemaining: number
  status: 'ACTIVE' | 'INACTIVE'
}

type ApiError = { message?: string; errors?: Record<string, string> }

function toDateTimeLocalInputValue(date: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}`
}

const CSRF_COOKIE_NAME = 'XSRF-TOKEN'
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN'
const SAFE_HTTP_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

function readCookie(name: string): string | null {
  const prefix = `${name}=`
  for (const part of document.cookie.split(';')) {
    const trimmed = part.trim()
    if (trimmed.startsWith(prefix)) {
      return trimmed.substring(prefix.length)
    }
  }
  return null
}

async function ensureCsrfToken(): Promise<string | null> {
  const existing = readCookie(CSRF_COOKIE_NAME)
  if (existing) return decodeURIComponent(existing)

  await fetch('/csrf', { credentials: 'same-origin' })
  const token = readCookie(CSRF_COOKIE_NAME)
  return token ? decodeURIComponent(token) : null
}

async function fetchJson<T>(url: string, init?: RequestInit): Promise<{ ok: true; data: T } | { ok: false; error: string }> {
  const method = (init?.method ?? 'GET').toUpperCase()
  const headers = new Headers(init?.headers)

  if (!SAFE_HTTP_METHODS.has(method)) {
    const csrfToken = await ensureCsrfToken()
    if (csrfToken) headers.set(CSRF_HEADER_NAME, csrfToken)
  }

  const res = await fetch(url, {
    ...init,
    method,
    headers,
    credentials: init?.credentials ?? 'same-origin',
  })
  const text = await res.text()

  let json: unknown = null
  try {
    json = text ? JSON.parse(text) : null
  } catch {
    json = null
  }

  if (!res.ok) {
    const apiErr = (json ?? {}) as ApiError
    return { ok: false, error: apiErr.message ?? `HTTP ${res.status}` }
  }

  return { ok: true, data: (json as T) }
}

function Badge({ tone, children }: { tone: 'ok' | 'warn' | 'danger' | 'muted'; children: ReactNode }) {
  const cls =
    tone === 'ok'
      ? 'border-[color:var(--ok)]/30 bg-[color:var(--ok)]/15 text-[color:var(--ok)]'
      : tone === 'warn'
        ? 'border-[color:var(--warn)]/30 bg-[color:var(--warn)]/15 text-[color:var(--warn)]'
        : tone === 'danger'
          ? 'border-[color:var(--danger)]/30 bg-[color:var(--danger)]/15 text-[color:var(--danger)]'
          : 'border-[color:var(--border)] bg-[color:var(--panel)] text-[color:var(--muted)]'

  return <span className={`inline-flex items-center rounded-full border px-2 py-1 text-xs font-medium ${cls}`}>{children}</span>
}

function Panel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-2xl border border-[color:var(--border)] bg-[color:var(--panel)] p-5 shadow-[0_20px_50px_rgba(0,0,0,0.35)]">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-sm font-semibold tracking-wide text-[color:var(--muted)]">{title}</h2>
      </div>
      {children}
    </section>
  )
}

function Input({
  label,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  return (
    <label className="block">
      <div className="mb-1 text-xs font-medium text-[color:var(--muted)]">{label}</div>
      <input
        {...props}
        className={`w-full rounded-xl border border-[color:var(--border)] bg-[color:var(--panel-2)] px-3 py-2 text-sm text-[color:var(--text)] placeholder:text-[color:var(--muted)] outline-none ring-0 focus:border-[color:var(--accent)]/60 focus:outline-none ${props.className ?? ''}`}
      />
    </label>
  )
}

function Select({
  label,
  children,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement> & { label: string; children: ReactNode }) {
  return (
    <label className="block">
      <div className="mb-1 text-xs font-medium text-[color:var(--muted)]">{label}</div>
      <select
        {...props}
        className={`w-full rounded-xl border border-[color:var(--border)] bg-[color:var(--panel-2)] px-3 py-2 text-sm text-[color:var(--text)] outline-none ring-0 focus:border-[color:var(--accent)]/60 focus:outline-none ${props.className ?? ''}`}
      >
        {children}
      </select>
    </label>
  )
}

function Button({ children, ...props }: ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      {...props}
      className={`inline-flex items-center justify-center rounded-xl border border-[color:var(--border)] bg-[color:var(--panel-2)] px-3 py-2 text-sm font-semibold text-[color:var(--text)] hover:border-[color:var(--accent)]/60 disabled:opacity-60 ${props.className ?? ''}`}
    >
      {children}
    </button>
  )
}

function App() {
  const [health, setHealth] = useState<string>('unknown')
  const [healthError, setHealthError] = useState<string | null>(null)
  const [vouchers, setVouchers] = useState<VoucherPublic[]>([])
  const [vouchersError, setVouchersError] = useState<string | null>(null)
  const [lastRefresh, setLastRefresh] = useState<Date | null>(null)
  const [refreshing, setRefreshing] = useState(false)

  const [adminToken, setAdminToken] = useState('dev-admin-token')
  const [adminCode, setAdminCode] = useState('DEMO10')
  const [adminDiscountType, setAdminDiscountType] = useState<'PERCENT' | 'FIXED'>('FIXED')
  const [adminDiscountValue, setAdminDiscountValue] = useState(10)
  const [adminMinSpend, setAdminMinSpend] = useState<number | ''>('')
  const [adminQuotaTotal, setAdminQuotaTotal] = useState(5)

  const now = useMemo(() => new Date(), [])
  const [adminStartAt, setAdminStartAt] = useState(() => toDateTimeLocalInputValue(new Date(now.getTime() - 24 * 60 * 60 * 1000)))
  const [adminEndAt, setAdminEndAt] = useState(() => toDateTimeLocalInputValue(new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000)))
  const [adminResult, setAdminResult] = useState<{ ok: boolean; text: string } | null>(null)

  const [validateCode, setValidateCode] = useState('DEMO10')
  const [validateOrderAmount, setValidateOrderAmount] = useState(100)
  const [validateResult, setValidateResult] = useState<ValidateVoucherResponse | null>(null)
  const [validateError, setValidateError] = useState<string | null>(null)

  const [claimCode, setClaimCode] = useState('DEMO10')
  const [claimOrderId, setClaimOrderId] = useState('ORDER-001')
  const [claimOrderAmount, setClaimOrderAmount] = useState(100)
  const [claimResult, setClaimResult] = useState<ClaimVoucherResponse | null>(null)
  const [claimError, setClaimError] = useState<string | null>(null)

  async function refresh() {
    setRefreshing(true)
    setHealthError(null)
    setVouchersError(null)

    const [healthRes, vouchersRes] = await Promise.all([
      fetchJson<{ status: string }>('/actuator/health'),
      fetchJson<VoucherPublic[]>('/vouchers/active'),
    ])

    if (healthRes.ok) {
      setHealth(healthRes.data.status ?? 'unknown')
    } else {
      setHealth('down')
      setHealthError(healthRes.error)
    }

    if (vouchersRes.ok) {
      setVouchers(vouchersRes.data ?? [])
    } else {
      setVouchers([])
      setVouchersError(vouchersRes.error)
    }

    setLastRefresh(new Date())
    setRefreshing(false)
  }

  useEffect(() => {
    refresh().catch((e) => {
      setHealth('down')
      setHealthError(String(e))
      setRefreshing(false)
    })
  }, [])

  const healthTone = health === 'UP' ? 'ok' : health === 'DOWN' ? 'danger' : 'muted'

  async function onCreateVoucher(e: FormEvent) {
    e.preventDefault()
    setAdminResult(null)

    const payload = {
      code: adminCode,
      discountType: adminDiscountType,
      discountValue: adminDiscountValue,
      startAt: adminStartAt,
      endAt: adminEndAt,
      minSpend: adminMinSpend === '' ? null : adminMinSpend,
      quotaTotal: adminQuotaTotal,
    }

    const res = await fetchJson<CreateVoucherResponse>('/admin/vouchers', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Admin-Token': adminToken,
      },
      body: JSON.stringify(payload),
    })

    if (!res.ok) {
      setAdminResult({ ok: false, text: res.error })
      return
    }

    setAdminResult({ ok: true, text: `created voucher ${res.data.code} (remaining=${res.data.quotaRemaining})` })
    await refresh()
  }

  async function onValidateVoucher(e: FormEvent) {
    e.preventDefault()
    setValidateError(null)
    setValidateResult(null)

    const res = await fetchJson<ValidateVoucherResponse>('/vouchers/validate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: validateCode, orderAmount: validateOrderAmount }),
    })

    if (!res.ok) {
      setValidateError(res.error)
      return
    }

    setValidateResult(res.data)
  }

  async function onClaimVoucher(e: FormEvent) {
    e.preventDefault()
    setClaimError(null)
    setClaimResult(null)

    const res = await fetchJson<ClaimVoucherResponse>('/vouchers/claim', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: claimCode, orderId: claimOrderId, orderAmount: claimOrderAmount }),
    })

    if (!res.ok) {
      setClaimError(res.error)
      return
    }

    setClaimResult(res.data)
    await refresh()
  }

  return (
    <div className="min-h-screen">
      <div className="mx-auto w-full max-w-6xl px-5 py-10">
        <header className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <div className="text-xs font-semibold tracking-[0.18em] text-[color:var(--muted)]">CONNECTIVITY TEST APP</div>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight">Voucher + Promo MVP</h1>
            <p className="mt-2 max-w-2xl text-sm text-[color:var(--muted)]">
              Frontend calls Spring Boot APIs, which talk to MySQL. This page is intentionally small, but it proves end-to-end wiring.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <Badge tone={healthTone}>{health === 'unknown' ? 'HEALTH: UNKNOWN' : `HEALTH: ${health}`}</Badge>
            <Button type="button" onClick={() => refresh()} disabled={refreshing}>
              {refreshing ? 'Refreshing…' : 'Refresh'}
            </Button>
          </div>
        </header>

        <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
          <Panel title="Backend / DB Status">
            <div className="space-y-3 text-sm">
              <div className="flex items-center justify-between gap-3">
                <div className="text-[color:var(--muted)]">Backend health</div>
                <div className="font-semibold">{health}</div>
              </div>
              {healthError ? <div className="rounded-xl border border-[color:var(--danger)]/30 bg-[color:var(--danger)]/10 p-3 text-xs">{healthError}</div> : null}

              <div className="flex items-center justify-between gap-3">
                <div className="text-[color:var(--muted)]">Active vouchers (DB)</div>
                <div className="font-semibold">{vouchers.length}</div>
              </div>
              {vouchersError ? <div className="rounded-xl border border-[color:var(--danger)]/30 bg-[color:var(--danger)]/10 p-3 text-xs">{vouchersError}</div> : null}

              <div className="text-xs text-[color:var(--muted)]">
                Last refresh: {lastRefresh ? lastRefresh.toLocaleString() : 'never'}
              </div>
            </div>
          </Panel>

          <Panel title="Admin: Create Voucher (Demo)">
            <form onSubmit={onCreateVoucher} className="space-y-3">
              <Input label="Admin token (X-Admin-Token)" value={adminToken} onChange={(e) => setAdminToken(e.target.value)} />
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <Input label="Code" value={adminCode} onChange={(e) => setAdminCode(e.target.value)} />
                <Select
                  label="Discount type"
                  value={adminDiscountType}
                  onChange={(e) => setAdminDiscountType(e.target.value === 'PERCENT' ? 'PERCENT' : 'FIXED')}
                >
                  <option value="FIXED">FIXED</option>
                  <option value="PERCENT">PERCENT</option>
                </Select>
                <Input
                  label={adminDiscountType === 'PERCENT' ? 'Discount value (%)' : 'Discount value (amount)'}
                  type="number"
                  step="0.01"
                  value={adminDiscountValue}
                  onChange={(e) => setAdminDiscountValue(Number(e.target.value))}
                />
                <Input
                  label="Min spend (optional)"
                  type="number"
                  step="0.01"
                  value={adminMinSpend}
                  onChange={(e) => setAdminMinSpend(e.target.value === '' ? '' : Number(e.target.value))}
                />
                <Input
                  label="Quota total"
                  type="number"
                  min={1}
                  value={adminQuotaTotal}
                  onChange={(e) => setAdminQuotaTotal(Number(e.target.value))}
                />
              </div>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <Input label="Start at" type="datetime-local" value={adminStartAt} onChange={(e) => setAdminStartAt(e.target.value)} />
                <Input label="End at" type="datetime-local" value={adminEndAt} onChange={(e) => setAdminEndAt(e.target.value)} />
              </div>
              <Button type="submit">Create voucher</Button>
              {adminResult ? (
                <div className={`rounded-xl border p-3 text-xs ${adminResult.ok ? 'border-[color:var(--ok)]/30 bg-[color:var(--ok)]/10' : 'border-[color:var(--danger)]/30 bg-[color:var(--danger)]/10'}`}>
                  {adminResult.text}
                </div>
              ) : null}
            </form>
          </Panel>

          <Panel title="Validate + Claim">
            <div className="space-y-6">
              <form onSubmit={onValidateVoucher} className="space-y-3">
                <div className="text-xs font-semibold tracking-wide text-[color:var(--muted)]">Validate</div>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <Input label="Code" value={validateCode} onChange={(e) => setValidateCode(e.target.value)} />
                  <Input label="Order amount" type="number" step="0.01" value={validateOrderAmount} onChange={(e) => setValidateOrderAmount(Number(e.target.value))} />
                </div>
                <Button type="submit">Validate</Button>
                {validateError ? <div className="rounded-xl border border-[color:var(--danger)]/30 bg-[color:var(--danger)]/10 p-3 text-xs">{validateError}</div> : null}
                {validateResult ? (
                  <div className="rounded-xl border border-[color:var(--border)] bg-[color:var(--panel-2)] p-3 text-xs">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge tone={validateResult.valid ? 'ok' : 'danger'}>{validateResult.valid ? 'VALID' : 'INVALID'}</Badge>
                      <span className="text-[color:var(--muted)]">{validateResult.message}</span>
                    </div>
                    <div className="mt-2 text-[color:var(--muted)]">
                      Discount: <span className="font-semibold text-[color:var(--text)]">{validateResult.discountAmount ?? '-'}</span>
                    </div>
                  </div>
                ) : null}
              </form>

              <form onSubmit={onClaimVoucher} className="space-y-3">
                <div className="text-xs font-semibold tracking-wide text-[color:var(--muted)]">Claim</div>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <Input label="Code" value={claimCode} onChange={(e) => setClaimCode(e.target.value)} />
                  <Input label="Order ID (idempotency key)" value={claimOrderId} onChange={(e) => setClaimOrderId(e.target.value)} />
                  <Input label="Order amount" type="number" step="0.01" value={claimOrderAmount} onChange={(e) => setClaimOrderAmount(Number(e.target.value))} />
                </div>
                <Button type="submit">Claim</Button>
                {claimError ? <div className="rounded-xl border border-[color:var(--danger)]/30 bg-[color:var(--danger)]/10 p-3 text-xs">{claimError}</div> : null}
                {claimResult ? (
                  <div className="rounded-xl border border-[color:var(--border)] bg-[color:var(--panel-2)] p-3 text-xs">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge tone={claimResult.success ? 'ok' : 'danger'}>{claimResult.success ? 'SUCCESS' : 'FAILED'}</Badge>
                      {claimResult.success && claimResult.idempotent ? <Badge tone="warn">IDEMPOTENT</Badge> : null}
                      <span className="text-[color:var(--muted)]">{claimResult.message}</span>
                    </div>
                    <div className="mt-2 grid grid-cols-2 gap-2 text-[color:var(--muted)]">
                      <div>Discount</div>
                      <div className="text-right font-semibold text-[color:var(--text)]">{claimResult.discountApplied ?? '-'}</div>
                      <div>Remaining</div>
                      <div className="text-right font-semibold text-[color:var(--text)]">{claimResult.quotaRemaining ?? '-'}</div>
                    </div>
                  </div>
                ) : null}
              </form>
            </div>
          </Panel>
        </div>

        <Panel title="Active Vouchers (DB-backed)">
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="text-xs text-[color:var(--muted)]">
                <tr>
                  <th className="py-2 pr-3 font-semibold">Code</th>
                  <th className="py-2 pr-3 font-semibold">Type</th>
                  <th className="py-2 pr-3 font-semibold">Value</th>
                  <th className="py-2 pr-3 font-semibold">Min spend</th>
                  <th className="py-2 pr-3 font-semibold">Remaining</th>
                  <th className="py-2 pr-3 font-semibold">Window</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[color:var(--border)]/70">
                {vouchers.length === 0 ? (
                  <tr>
                    <td className="py-4 text-[color:var(--muted)]" colSpan={6}>
                      No active vouchers found (create one above).
                    </td>
                  </tr>
                ) : (
                  vouchers.map((v) => (
                    <tr key={v.code} className="hover:bg-white/5">
                      <td className="py-3 pr-3 font-semibold">{v.code}</td>
                      <td className="py-3 pr-3 text-[color:var(--muted)]">{v.discountType}</td>
                      <td className="py-3 pr-3 text-[color:var(--muted)]">{v.discountValue}</td>
                      <td className="py-3 pr-3 text-[color:var(--muted)]">{v.minSpend ?? '-'}</td>
                      <td className="py-3 pr-3">
                        <Badge tone={v.quotaRemaining > 0 ? 'ok' : 'danger'}>{v.quotaRemaining}</Badge>
                      </td>
                      <td className="py-3 pr-3 text-xs text-[color:var(--muted)]">
                        <div>{v.startAt}</div>
                        <div>{v.endAt}</div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </Panel>

        <footer className="mt-8 text-xs text-[color:var(--muted)]">
          Dev mode uses a Vite proxy to `http://localhost:8080` for `/actuator`, `/vouchers`, and `/admin`.
        </footer>
      </div>
    </div>
  )
}

export default App
